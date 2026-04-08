package com.ft.ftchinese.repository

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ft.ftchinese.R

data class NotificationSettingStatus(
    val enabled: Boolean,
    val permissionGranted: Boolean,
    val appNotificationsEnabled: Boolean,
    val channelEnabled: Boolean,
)

object NotificationSettingsHelper {
    fun readStatus(context: Context): NotificationSettingStatus {
        val permissionGranted = isPermissionGranted(context)
        val appNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        val channelEnabled = isNewsChannelEnabled(context)

        return NotificationSettingStatus(
            enabled = permissionGranted && appNotificationsEnabled && channelEnabled,
            permissionGranted = permissionGranted,
            appNotificationsEnabled = appNotificationsEnabled,
            channelEnabled = channelEnabled,
        )
    }

    fun openSystemNotificationSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.fromParts("package", context.packageName, null)
            }
        }

        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun canRequestRuntimePermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !isPermissionGranted(context)
    }

    private fun isPermissionGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }

        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isNewsChannelEnabled(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return true
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return true
        val channel = manager.getNotificationChannel(context.getString(R.string.news_notification_channel_id))
            ?: return true
        return channel.importance != NotificationManager.IMPORTANCE_NONE
    }
}
