package com.ft.ftchinese.ui.settings.fcm

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ft.ftchinese.repository.NotificationSettingsHelper
import com.ft.ftchinese.ui.components.ProgressLayout

@Composable
fun FcmActivityScreen() {
    val context = LocalContext.current
    val fcmState = rememberFcmState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        fcmState.refreshNotificationStatus(triggerPushSyncIfChanged = true)
    }

    DisposableEffect(lifecycleOwner, fcmState) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                fcmState.refreshNotificationStatus(triggerPushSyncIfChanged = true)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    ProgressLayout(
        loading = fcmState.progress,
        modifier = Modifier.fillMaxSize()
    ) {
        FcmScreen(
            loading = fcmState.progress,
            notificationStatus = fcmState.notificationStatus,
            hasPromptedOnce = fcmState.hasPromptedOnce(),
            messageRows = fcmState.messages,
            onToggleNotification = { enabled ->
                if (enabled) {
                    if (fcmState.shouldRequestRuntimePermission()) {
                        fcmState.markPromptedOnce()
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        launchNotificationSetting(context)
                    }
                } else {
                    launchNotificationSetting(context)
                }
            },
            onSetting = {
                launchNotificationSetting(context)
            },
            onCheck = fcmState::checkFcm,
        )
    }

}

private fun launchNotificationSetting(context: Context) {
    NotificationSettingsHelper.openSystemNotificationSettings(context)
}
