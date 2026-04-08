package com.ft.ftchinese.service

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ft.ftchinese.R
import com.ft.ftchinese.R.string.news_notification_channel_id
import com.ft.ftchinese.repository.PushClient
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

private const val TAG = "NewsMessagingService"
private const val LOG_PREFIX = "[FTCPush]"

class NewsMessagingService : FirebaseMessagingService() {
    // Handle messages when app is in foreground.
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.i(TAG, "From: ${remoteMessage.from}")

        // This are the custom data.
        // {action=story, pageId=001084989}
        // The data structure is identical for Topic message.
        remoteMessage.data.isNotEmpty().let {
            Log.i(TAG, "Message data payload: " + remoteMessage.data)
        }

        remoteMessage.notification?.let {
            Log.i(TAG, "Title: ${it.title}, Body: ${it.body}")
        }

        handleNow(remoteMessage.notification, remoteMessage.data)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        Log.i(TAG, "$LOG_PREFIX firebase_on_new_token token=${PushClient.summarizeSecret(token)}")

        // Push registration is now done through /api/push/register, not piggybacked
        // onto auth payloads. Keep the FCM token separate from TokenManager's device id.
        PushClient.handleNewFcmToken(token)
    }


    private fun handleNow(notification: RemoteMessage.Notification?, data: Map<String, String>?) {
        if (notification == null || data == null || data.isEmpty()) {
            Log.i(TAG, "$LOG_PREFIX message_ignored reason=missing_notification_or_data")
            return
        }

        val route = runCatching {
            PushNotificationRouter.routeFromData(
                data = data,
                fallbackTitle = notification.title,
            )
        }.getOrElse { error ->
            Log.e(
                TAG,
                "$LOG_PREFIX message_ignored reason=push_route_exception message=${error.message} data=$data",
                error
            )
            null
        } ?: run {
            Log.i(
                TAG,
                "$LOG_PREFIX message_ignored reason=unsupported_push_route data=$data"
            )
            return
        }
        val intent = runCatching {
            PushNotificationRouter.createPendingIntent(this, route)
        }.getOrElse { error ->
            Log.e(
                TAG,
                "$LOG_PREFIX message_ignored reason=pending_intent_exception action=${route.action} targetId=${route.targetId} message=${error.message}",
                error
            )
            null
        } ?: return

        val builder = NotificationCompat.Builder(
                applicationContext,
                getString(news_notification_channel_id))
                .setSmallIcon(R.drawable.logo_round)
                .setContentTitle(notification.title)
                .setStyle(NotificationCompat.BigTextStyle()
                        .bigText(notification.body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(intent)

        with(NotificationManagerCompat.from(this)) {
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(
                    TAG,
                    "$LOG_PREFIX notification_display_skipped reason=missing_post_notifications_permission action=${route.action} targetId=${route.targetId}"
                )
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            notify(1, builder.build())
            Log.i(
                TAG,
                "$LOG_PREFIX notification_displayed action=${route.action} targetId=${route.targetId}"
            )
        }
    }
}
