package com.ft.ftchinese.ui.main

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ft.ftchinese.R
import com.ft.ftchinese.repository.NotificationSettingsHelper
import com.ft.ftchinese.repository.PushClient
import com.ft.ftchinese.store.NotificationPermissionStore
import com.ft.ftchinese.ui.components.SimpleDialog

@Composable
fun NotificationPermissionPromptOnLaunch(
    isLoggedIn: Boolean,
) {
    val context = LocalContext.current
    val permissionStore = remember(context) {
        NotificationPermissionStore.getInstance(context)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        PushClient.syncRegistration()
    }

    val shouldShowLaunchRationale = isLoggedIn
        && NotificationSettingsHelper.canRequestRuntimePermission(context)
        && !permissionStore.hasShownLaunchRationaleOnce()
        && !permissionStore.hasPromptedOnce()
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(shouldShowLaunchRationale) {
        showDialog = shouldShowLaunchRationale
    }

    if (showDialog) {
        SimpleDialog(
            title = stringResource(id = R.string.notification_permission_prompt_title),
            body = stringResource(id = R.string.notification_permission_prompt_body),
            onDismiss = {
                permissionStore.markLaunchRationaleShownOnce()
                showDialog = false
            },
            onConfirm = {
                permissionStore.markLaunchRationaleShownOnce()
                permissionStore.markPromptedOnce()
                showDialog = false
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            confirmText = stringResource(id = R.string.notification_permission_prompt_confirm),
            dismissText = stringResource(id = R.string.notification_permission_prompt_dismiss),
        )
    }
}
