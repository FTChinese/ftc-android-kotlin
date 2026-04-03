package com.ft.ftchinese.repository

import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Locale

private const val TAG = "PushClient"
private const val PUSH_PLATFORM_ANDROID = "android"
private const val PUSH_PROVIDER_FCM = "fcm"
private const val NOTIFICATION_GRANTED = "granted"
private const val NOTIFICATION_DENIED = "denied"

object PushClient {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Login and session flows still use TokenManager as the stable installation id.
     * Remote push registration uses the real FCM token separately via /push/register.
     */
    fun syncFcmRegistration() {
        val context = App.instance
        if (!isGmsAvailable(context)) {
            return
        }

        val userId = SessionManager
            .getInstance(context)
            .loadAccount(raw = true)
            ?.id
            ?.takeIf { it.isNotBlank() }
            ?: return

        val store = PushRegistrationStore.getInstance(context)
        val cachedToken = store.loadFcmToken()
        if (!cachedToken.isNullOrBlank()) {
            registerFcmTokenIfNeeded(
                userId = userId,
                token = cachedToken,
            )
            return
        }

        FirebaseMessaging
            .getInstance()
            .token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Unable to retrieve FCM token", task.exception)
                    return@addOnCompleteListener
                }

                val token = task.result?.takeIf { it.isNotBlank() } ?: return@addOnCompleteListener
                handleNewFcmToken(token)
            }
    }

    fun handleNewFcmToken(token: String) {
        if (token.isBlank()) {
            return
        }

        val context = App.instance
        val store = PushRegistrationStore.getInstance(context)
        store.saveFcmToken(token)

        if (!isGmsAvailable(context)) {
            return
        }

        val userId = SessionManager
            .getInstance(context)
            .loadAccount(raw = true)
            ?.id
            ?.takeIf { it.isNotBlank() }
            ?: return

        registerFcmTokenIfNeeded(
            userId = userId,
            token = token,
        )
    }

    internal fun needsFcmRegistration(
        currentUserId: String,
        currentToken: String,
        lastUserId: String?,
        lastToken: String?,
    ): Boolean {
        return currentUserId != lastUserId || currentToken != lastToken
    }

    internal fun buildFcmRegistration(
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
            provider = PUSH_PROVIDER_FCM,
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

    private fun registerFcmTokenIfNeeded(
        userId: String,
        token: String,
    ) {
        val context = App.instance
        val store = PushRegistrationStore.getInstance(context)

        if (!needsFcmRegistration(
                currentUserId = userId,
                currentToken = token,
                lastUserId = store.loadLastRegisteredUserId(),
                lastToken = store.loadLastRegisteredFcmToken(),
            )
        ) {
            return
        }

        val request = buildFcmRegistration(
            pushId = token,
            installationId = TokenManager.getInstance(context).getToken(),
            appVersion = BuildConfig.VERSION_NAME,
            locale = Locale.getDefault().toLanguageTag(),
            brand = Build.BRAND ?: "",
            model = Build.MODEL ?: "",
            osVersion = Build.VERSION.RELEASE ?: "",
            notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled(),
            gmsAvailable = isGmsAvailable(context),
        )

        scope.launch {
            runCatching {
                registerPush(
                    userId = userId,
                    request = request,
                )
            }.onSuccess {
                store.markFcmRegistered(
                    userId = userId,
                    token = token,
                )
                Log.i(TAG, "Registered FCM token for user $userId")
            }.onFailure {
                Log.w(TAG, "Failed to register FCM token", it)
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

    private fun isGmsAvailable(context: App): Boolean {
        return GoogleApiAvailability
            .getInstance()
            .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
    }
}
