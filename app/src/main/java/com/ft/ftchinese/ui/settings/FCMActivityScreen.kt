package com.ft.ftchinese.ui.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.ProgressLayout

@Composable
fun FcmActivityScreen(
    settingsViewModel: SettingsViewModel
) {
    val context = LocalContext.current
    val progress by settingsViewModel.progressLiveData.observeAsState(false)
    val fcmStatus by settingsViewModel.fcmStatusLiveData.observeAsState(listOf())

    ProgressLayout(
        loading = progress
    ) {
        FcmScreen(
            loading = progress,
            messageRows = fcmStatus,
            onSetting = {
                launchNotificationSetting(context)
            },
            onCheck = settingsViewModel::checkFcm,
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






