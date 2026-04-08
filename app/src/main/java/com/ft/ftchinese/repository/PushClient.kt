package com.ft.ftchinese.repository

import android.os.Build
import android.util.Log
import com.ft.ftchinese.App
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.request.NativePushRegistration
import com.ft.ftchinese.store.PushRegistrationStore
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.store.TokenManager
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.messaging.FirebaseMessaging
import com.vivo.push.PushClient as VivoSystemPushClient
import com.vivo.push.ups.UPSRegisterCallback
import com.vivo.push.ups.UPSTurnCallback
import com.vivo.push.ups.VUpsManager
import com.vivo.push.ups.CodeResult
import com.vivo.push.ups.TokenResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.Locale

private const val TAG = "PushClient"
// Shared log prefix so logcat can isolate the whole native push registration flow.
private const val LOG_PREFIX = "[FTCPush]"
private const val PUSH_PLATFORM_ANDROID = "android"
private const val PUSH_PROVIDER_FCM = "fcm"
private const val PUSH_PROVIDER_VIVO = "vivo"
private const val NOTIFICATION_GRANTED = "granted"
private const val NOTIFICATION_DENIED = "denied"

object PushClient {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Login and session flows still use TokenManager as the stable installation id.
     * Remote push registration uses provider-native push ids separately via /push/register.
     *
     * The backend currently stores one active Android push provider per user, so we choose
     * a single provider at runtime: vivo/iQOO devices prefer vivo push when configured,
     * otherwise the app falls back to FCM.
     */
    fun syncRegistration() {
        val context = App.instance
        val preferredProvider = resolvePreferredProvider(
            gmsAvailable = isGmsAvailable(context),
            brand = Build.BRAND ?: "",
            manufacturer = Build.MANUFACTURER ?: "",
            vivoConfigured = hasVivoPushConfig(),
        )
        val allowVivoFallback = preferredProvider == PUSH_PROVIDER_FCM && shouldUseVivoProvider(
            brand = Build.BRAND ?: "",
            manufacturer = Build.MANUFACTURER ?: "",
            vivoConfigured = hasVivoPushConfig(),
        )

        logInfo(
            "sync_strategy",
            "preferred=$preferredProvider allowVivoFallback=$allowVivoFallback gmsAvailable=${isGmsAvailable(context)} brand=${Build.BRAND} manufacturer=${Build.MANUFACTURER}"
        )

        when (preferredProvider) {
            PUSH_PROVIDER_VIVO -> syncVivoRegistration(context, allowFcmFallback = true)
            else -> syncFcmRegistration(context, allowVivoFallback = allowVivoFallback)
        }
    }

    fun handleNewFcmToken(token: String) {
        if (token.isBlank()) {
            logInfo("fcm_on_new_token_ignored", "reason=blank")
            return
        }

        logInfo("fcm_on_new_token_received", "token=${summarizeSecret(token)}")
        val context = App.instance
        val store = PushRegistrationStore.getInstance(context)
        store.saveFcmToken(token)
        logInfo("fcm_on_new_token_cached", "token=${summarizeSecret(token)}")

        val preferredProvider = resolvePreferredProvider(
            gmsAvailable = isGmsAvailable(context),
            brand = Build.BRAND ?: "",
            manufacturer = Build.MANUFACTURER ?: "",
            vivoConfigured = hasVivoPushConfig(),
        )
        if (preferredProvider != PUSH_PROVIDER_FCM) {
            logInfo(
                "fcm_on_new_token_skip",
                "reason=provider_not_preferred preferred=$preferredProvider token=${summarizeSecret(token)}"
            )
            return
        }

        val userId = currentUserId(context)
            ?: run {
                logInfo(
                    "fcm_on_new_token_skip",
                    "reason=missing_user token=${summarizeSecret(token)}"
                )
                return
            }

        registerPushIfNeeded(
            userId = userId,
            provider = PUSH_PROVIDER_FCM,
            pushId = token,
        )
    }

    fun handleNewVivoPushId(pushId: String) {
        if (pushId.isBlank()) {
            logInfo("vivo_reg_id_ignored", "reason=blank")
            return
        }

        logInfo("vivo_reg_id_received", "pushId=${summarizeSecret(pushId)}")
        val context = App.instance
        val store = PushRegistrationStore.getInstance(context)
        store.saveVivoPushId(pushId)
        logInfo("vivo_reg_id_cached", "pushId=${summarizeSecret(pushId)}")

        val preferredProvider = resolvePreferredProvider(
            gmsAvailable = isGmsAvailable(context),
            brand = Build.BRAND ?: "",
            manufacturer = Build.MANUFACTURER ?: "",
            vivoConfigured = hasVivoPushConfig(),
        )
        if (preferredProvider != PUSH_PROVIDER_VIVO) {
            logInfo(
                "vivo_reg_id_skip",
                "reason=provider_not_preferred preferred=$preferredProvider pushId=${summarizeSecret(pushId)}"
            )
            return
        }

        val userId = currentUserId(context)
            ?: run {
                logInfo(
                    "vivo_reg_id_skip",
                    "reason=missing_user pushId=${summarizeSecret(pushId)}"
                )
                return
            }

        registerPushIfNeeded(
            userId = userId,
            provider = PUSH_PROVIDER_VIVO,
            pushId = pushId,
        )
    }

    internal fun needsRegistration(
        currentUserId: String,
        currentProvider: String,
        currentPushId: String,
        currentNotificationPermission: String,
        lastUserId: String?,
        lastProvider: String?,
        lastPushId: String?,
        lastNotificationPermission: String?,
    ): Boolean {
        return currentUserId != lastUserId
            || currentProvider != lastProvider
            || currentPushId != lastPushId
            || currentNotificationPermission != lastNotificationPermission
    }

    internal fun shouldUseVivoProvider(
        brand: String,
        manufacturer: String,
        vivoConfigured: Boolean,
    ): Boolean {
        if (!vivoConfigured) {
            return false
        }

        val brandLower = brand.trim().lowercase(Locale.US)
        val manufacturerLower = manufacturer.trim().lowercase(Locale.US)
        val isVivoBrand = listOf(brandLower, manufacturerLower).any {
            it.contains("vivo") || it.contains("iqoo")
        }

        return isVivoBrand
    }

    internal fun buildRegistration(
        provider: String,
        pushId: String,
        installationId: String,
        appVersion: String,
        locale: String,
        brand: String,
        model: String,
        osVersion: String,
        notificationsEnabled: Boolean,
        gmsAvailable: Boolean,
    ): NativePushRegistration {
        return NativePushRegistration(
            platform = PUSH_PLATFORM_ANDROID,
            provider = provider,
            pushId = pushId,
            installationId = installationId,
            brand = brand,
            model = model,
            osVersion = osVersion,
            appVersion = appVersion,
            locale = locale,
            notificationPermission = if (notificationsEnabled) {
                NOTIFICATION_GRANTED
            } else {
                NOTIFICATION_DENIED
            },
            gmsAvailable = gmsAvailable,
        )
    }

    internal fun summarizeSecret(value: String): String {
        if (value.isBlank()) {
            return "empty"
        }

        return "${maskValue(value, head = 8, tail = 4)} hash=${hashPrefix(value)}"
    }

    private fun syncFcmRegistration(context: App, allowVivoFallback: Boolean = false) {
        val gmsAvailable = isGmsAvailable(context)
        logInfo(
            "sync_start",
            "provider=$PUSH_PROVIDER_FCM gmsAvailable=$gmsAvailable allowVivoFallback=$allowVivoFallback"
        )
        if (!gmsAvailable) {
            logInfo("sync_skip", "provider=$PUSH_PROVIDER_FCM reason=gms_unavailable")
            if (allowVivoFallback) {
                logInfo("sync_fallback", "from=$PUSH_PROVIDER_FCM to=$PUSH_PROVIDER_VIVO reason=gms_unavailable")
                syncVivoRegistration(context, allowFcmFallback = false)
            }
            return
        }

        val userId = currentUserId(context)
            ?: run {
                logInfo("sync_skip", "provider=$PUSH_PROVIDER_FCM reason=missing_user")
                return
            }

        val store = PushRegistrationStore.getInstance(context)
        val cachedToken = store.loadFcmToken()
        if (!cachedToken.isNullOrBlank()) {
            logInfo(
                "sync_use_cached_token",
                "provider=$PUSH_PROVIDER_FCM userId=${maskValue(userId)} token=${summarizeSecret(cachedToken)}"
            )
            registerPushIfNeeded(
                userId = userId,
                provider = PUSH_PROVIDER_FCM,
                pushId = cachedToken,
            )
            return
        }

        logInfo(
            "sync_fetch_token_start",
            "provider=$PUSH_PROVIDER_FCM userId=${maskValue(userId)}"
        )
        FirebaseMessaging
            .getInstance()
            .token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    logWarn(
                        event = "sync_fetch_token_failed",
                        details = "provider=$PUSH_PROVIDER_FCM userId=${maskValue(userId)} message=${task.exception?.message ?: ""}",
                        throwable = task.exception
                    )
                    if (allowVivoFallback) {
                        logInfo(
                            "sync_fallback",
                            "from=$PUSH_PROVIDER_FCM to=$PUSH_PROVIDER_VIVO reason=token_fetch_failed userId=${maskValue(userId)}"
                        )
                        syncVivoRegistration(context, allowFcmFallback = false)
                    }
                    return@addOnCompleteListener
                }

                val token = task.result?.takeIf { it.isNotBlank() } ?: run {
                    logInfo(
                        "sync_fetch_token_empty",
                        "provider=$PUSH_PROVIDER_FCM userId=${maskValue(userId)}"
                    )
                    if (allowVivoFallback) {
                        logInfo(
                            "sync_fallback",
                            "from=$PUSH_PROVIDER_FCM to=$PUSH_PROVIDER_VIVO reason=empty_token userId=${maskValue(userId)}"
                        )
                        syncVivoRegistration(context, allowFcmFallback = false)
                    }
                    return@addOnCompleteListener
                }

                logInfo(
                    "sync_fetch_token_success",
                    "provider=$PUSH_PROVIDER_FCM userId=${maskValue(userId)} token=${summarizeSecret(token)}"
                )
                handleNewFcmToken(token)
            }
    }

    private fun syncVivoRegistration(context: App, allowFcmFallback: Boolean = true) {
        logInfo(
            "sync_start",
            "provider=$PUSH_PROVIDER_VIVO vivoConfigured=${hasVivoPushConfig()} allowFcmFallback=$allowFcmFallback brand=${Build.BRAND} manufacturer=${Build.MANUFACTURER}"
        )

        val userId = currentUserId(context)
            ?: run {
                logInfo("sync_skip", "provider=$PUSH_PROVIDER_VIVO reason=missing_user")
                return
            }

        if (!hasVivoPushConfig()) {
            logInfo("sync_skip", "provider=$PUSH_PROVIDER_VIVO reason=missing_vivo_config")
            if (allowFcmFallback) {
                logInfo("sync_fallback", "from=$PUSH_PROVIDER_VIVO to=$PUSH_PROVIDER_FCM reason=missing_vivo_config")
                syncFcmRegistration(context, allowVivoFallback = false)
            }
            return
        }

        val vivoSystemClient = runCatching {
            VivoSystemPushClient.getInstance(context)
        }.onFailure {
            logWarn(
                event = "vivo_client_unavailable",
                details = "userId=${maskValue(userId)} message=${it.message ?: ""}",
                throwable = it
            )
        }.getOrNull() ?: return

        runCatching {
            vivoSystemClient.checkManifest()
        }.onFailure {
            logWarn(
                event = "vivo_manifest_check_failed",
                details = "userId=${maskValue(userId)} message=${it.message ?: ""}",
                throwable = it
            )
            return
        }

        if (!vivoSystemClient.isSupport()) {
            logInfo("sync_skip", "provider=$PUSH_PROVIDER_VIVO reason=sdk_reports_unsupported")
            if (allowFcmFallback) {
                logInfo("sync_fallback", "from=$PUSH_PROVIDER_VIVO to=$PUSH_PROVIDER_FCM reason=sdk_reports_unsupported")
                syncFcmRegistration(context, allowVivoFallback = false)
            }
            return
        }

        val store = PushRegistrationStore.getInstance(context)
        val cachedPushId = store.loadVivoPushId()
        if (!cachedPushId.isNullOrBlank()) {
            logInfo(
                "sync_use_cached_token",
                "provider=$PUSH_PROVIDER_VIVO userId=${maskValue(userId)} pushId=${summarizeSecret(cachedPushId)}"
            )
            registerPushIfNeeded(
                userId = userId,
                provider = PUSH_PROVIDER_VIVO,
                pushId = cachedPushId,
            )
            return
        }

        logInfo(
            "vivo_turn_on_start",
            "userId=${maskValue(userId)} appId=${maskValue(BuildConfig.VIVO_PUSH_APP_ID)}"
        )
        VUpsManager.getInstance().turnOnPush(context, object : UPSTurnCallback {
            override fun onResult(codeResult: CodeResult) {
                val returnCode = codeResult.returnCode
                logInfo(
                    "vivo_turn_on_result",
                    "userId=${maskValue(userId)} code=$returnCode"
                )

                if (returnCode != 0 && returnCode != 1) {
                    return
                }

                requestVivoToken(context, userId)
            }
        })
    }

    private fun requestVivoToken(context: App, userId: String) {
        logInfo(
            "vivo_register_token_start",
            "userId=${maskValue(userId)} appId=${maskValue(BuildConfig.VIVO_PUSH_APP_ID)}"
        )

        VUpsManager.getInstance().registerToken(
            context,
            BuildConfig.VIVO_PUSH_APP_ID,
            BuildConfig.VIVO_PUSH_APP_KEY,
            BuildConfig.VIVO_PUSH_APP_SECRET,
            object : UPSRegisterCallback {
                override fun onResult(tokenResult: TokenResult) {
                    val returnCode = tokenResult.returnCode
                    val pushId = tokenResult.token?.trim().orEmpty()

                    if (returnCode != 0 || pushId.isBlank()) {
                        logWarn(
                            event = "vivo_register_token_failed",
                            details = "userId=${maskValue(userId)} code=$returnCode pushId=${summarizeSecret(pushId)}"
                        )
                        return
                    }

                    logInfo(
                        "vivo_register_token_success",
                        "userId=${maskValue(userId)} pushId=${summarizeSecret(pushId)}"
                    )
                    handleNewVivoPushId(pushId)
                }
            }
        )
    }

    private fun registerPushIfNeeded(
        userId: String,
        provider: String,
        pushId: String,
    ) {
        val context = App.instance
        val store = PushRegistrationStore.getInstance(context)
        val lastUserId = store.loadLastRegisteredUserId()
        val lastProvider = store.loadLastRegisteredProvider()
        val lastPushId = store.loadLastRegisteredPushId()
        val lastNotificationPermission = store.loadLastNotificationPermission()
        val notificationStatus = NotificationSettingsHelper.readStatus(context)
        val currentNotificationPermission = if (notificationStatus.enabled) {
            NOTIFICATION_GRANTED
        } else {
            NOTIFICATION_DENIED
        }

        if (!needsRegistration(
                currentUserId = userId,
                currentProvider = provider,
                currentPushId = pushId,
                currentNotificationPermission = currentNotificationPermission,
                lastUserId = lastUserId,
                lastProvider = lastProvider,
                lastPushId = lastPushId,
                lastNotificationPermission = lastNotificationPermission,
            )
        ) {
            logInfo(
                "register_skip",
                "reason=already_registered provider=$provider userId=${maskValue(userId)} pushId=${summarizeSecret(pushId)} notificationPermission=$currentNotificationPermission"
            )
            return
        }

        val installationId = TokenManager.getInstance(context).getToken()
        val gmsAvailable = isGmsAvailable(context)
        val request = buildRegistration(
            provider = provider,
            pushId = pushId,
            installationId = installationId,
            appVersion = BuildConfig.VERSION_NAME,
            locale = Locale.getDefault().toLanguageTag(),
            brand = Build.BRAND ?: "",
            model = Build.MODEL ?: "",
            osVersion = Build.VERSION.RELEASE ?: "",
            notificationsEnabled = notificationStatus.enabled,
            gmsAvailable = gmsAvailable,
        )

        logInfo(
            "register_request_start",
            "provider=$provider userId=${maskValue(userId)} installationId=${maskValue(installationId)} pushId=${summarizeSecret(pushId)} notificationsEnabled=${notificationStatus.enabled} appNotificationsEnabled=${notificationStatus.appNotificationsEnabled} channelEnabled=${notificationStatus.channelEnabled} permissionGranted=${notificationStatus.permissionGranted} gmsAvailable=$gmsAvailable"
        )
        scope.launch {
            runCatching {
                registerPush(
                    userId = userId,
                    request = request,
                )
            }.onSuccess {
                store.markRegistered(
                    provider = provider,
                    userId = userId,
                    pushId = pushId,
                    notificationPermission = currentNotificationPermission,
                )
                logInfo(
                    "register_request_success",
                    "provider=$provider userId=${maskValue(userId)} installationId=${maskValue(installationId)} pushId=${summarizeSecret(pushId)} notificationPermission=$currentNotificationPermission"
                )
            }.onFailure {
                logWarn(
                    event = "register_request_failed",
                    details = "provider=$provider userId=${maskValue(userId)} installationId=${maskValue(installationId)} pushId=${summarizeSecret(pushId)} message=${it.message ?: ""}",
                    throwable = it
                )
            }
        }
    }

    private fun registerPush(
        userId: String,
        request: NativePushRegistration,
    ) {
        Fetch()
            .post(Endpoint.pushRegister)
            .noCache()
            .setApiKey()
            .setClient()
            .setUserId(userId)
            .sendJson(request)
            .endOrThrow()
    }

    private fun currentUserId(context: App): String? {
        return SessionManager
            .getInstance(context)
            .loadAccount(raw = true)
            ?.id
            ?.takeIf { it.isNotBlank() }
    }

    internal fun resolvePreferredProvider(
        gmsAvailable: Boolean,
        brand: String,
        manufacturer: String,
        vivoConfigured: Boolean,
    ): String {
        if (gmsAvailable) {
            return PUSH_PROVIDER_FCM
        }

        val useVivo = shouldUseVivoProvider(
            brand = brand,
            manufacturer = manufacturer,
            vivoConfigured = vivoConfigured,
        )

        if (useVivo) {
            return PUSH_PROVIDER_VIVO
        }

        return PUSH_PROVIDER_FCM
    }

    private fun hasVivoPushConfig(): Boolean {
        return BuildConfig.VIVO_PUSH_APP_ID.isNotBlank()
            && BuildConfig.VIVO_PUSH_APP_KEY.isNotBlank()
            && BuildConfig.VIVO_PUSH_APP_SECRET.isNotBlank()
    }

    private fun isGmsAvailable(context: App): Boolean {
        return GoogleApiAvailability
            .getInstance()
            .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
    }

    private fun hashPrefix(value: String): String {
        if (value.isBlank()) {
            return ""
        }

        return MessageDigest
            .getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(12)
    }

    private fun maskValue(value: String, head: Int = 6, tail: Int = 4): String {
        if (value.isBlank()) {
            return ""
        }

        if (value.length <= head + tail) {
            return value
        }

        return "${value.take(head)}...${value.takeLast(tail)}"
    }

    private fun logInfo(event: String, details: String) {
        Log.i(TAG, "$LOG_PREFIX $event $details")
    }

    private fun logWarn(event: String, details: String, throwable: Throwable? = null) {
        if (throwable == null) {
            Log.w(TAG, "$LOG_PREFIX $event $details")
            return
        }
        Log.w(TAG, "$LOG_PREFIX $event $details", throwable)
    }
}
