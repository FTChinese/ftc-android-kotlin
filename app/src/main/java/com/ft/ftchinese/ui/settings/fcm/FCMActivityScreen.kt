package com.ft.ftchinese.ui.settings.fcm

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.ProgressLayout

@Composable
fun FcmActivityScreen() {
    val context = LocalContext.current
    val fcmState = rememberFcmState()

    ProgressLayout(
        loading = fcmState.progress
    ) {
        FcmScreen(
            loading = fcmState.progress,
            messageRows = fcmState.messages,
            onSetting = {
                launchNotificationSetting(context)
            },
            onCheck = fcmState::checkFcm,
        )
    }

}

private fun launchNotificationSetting(context: Context) {
    val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, BuildConfig.APPLICATION_ID)
        putExtra(Settings.EXTRA_CHANNEL_ID, context.getString(R.string.news_notification_channel_id))
    }

    context.startActivity(intent)
}






