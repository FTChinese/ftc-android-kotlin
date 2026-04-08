package com.ft.ftchinese.ui.settings.fcm

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.repository.NotificationSettingStatus
import com.ft.ftchinese.repository.NotificationSettingsHelper
import com.ft.ftchinese.repository.PushClient
import com.ft.ftchinese.store.NotificationPermissionStore
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging

class FcmState(
    private val context: Context,
) {
    private val permissionStore = NotificationPermissionStore.getInstance(context)

    var progress by mutableStateOf(false)
        private set

    var messages by mutableStateOf(listOf<IconTextRow>())
        private set

    var notificationStatus by mutableStateOf(NotificationSettingsHelper.readStatus(context))
        private set

    fun refreshNotificationStatus(triggerPushSyncIfChanged: Boolean = false) {
        val latest = NotificationSettingsHelper.readStatus(context)
        val changed = latest != notificationStatus
        notificationStatus = latest

        if (changed && triggerPushSyncIfChanged) {
            PushClient.syncRegistration()
        }
    }

    fun hasPromptedOnce(): Boolean {
        return permissionStore.hasPromptedOnce()
    }

    fun markPromptedOnce() {
        permissionStore.markPromptedOnce()
    }

    fun shouldRequestRuntimePermission(): Boolean {
        return NotificationSettingsHelper.canRequestRuntimePermission(context) && !hasPromptedOnce()
    }

    fun checkFcm() {
        progress = true

        refreshNotificationStatus(triggerPushSyncIfChanged = false)

        val playAvailable = checkPlayServices()
        messages = FcmMessageBuilder()
            .addSystemNotification(notificationStatus)
            .addPlayService(playAvailable)
            .build()

        retrieveRegistrationToken {
            messages = FcmMessageBuilder()
                .addSystemNotification(notificationStatus)
                .addPlayService(playAvailable)
                .addTokenRetrievable(it.isSuccessful)
                .build()

            progress = false
        }
    }

    private fun checkPlayServices(): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)

        return resultCode == ConnectionResult.SUCCESS
    }

    private fun retrieveRegistrationToken(listener: OnCompleteListener<String>) {
        FirebaseMessaging
            .getInstance()
            .token
            .addOnCompleteListener(listener)
    }
}

@Composable
fun rememberFcmState(
    context: Context = LocalContext.current
) = remember {
    FcmState(context)
}
