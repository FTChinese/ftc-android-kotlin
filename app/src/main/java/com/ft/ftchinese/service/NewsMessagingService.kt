package com.ft.ftchinese.service

import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ft.ftchinese.R
import com.ft.ftchinese.R.string.news_notification_channel_id
import com.ft.ftchinese.model.content.*
import com.ft.ftchinese.model.enums.ArticleType
import com.ft.ftchinese.ui.article.ArticleActivity
import com.ft.ftchinese.ui.channel.ChannelActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

private const val TAG = "NewsMessagingService"

@kotlinx.coroutines.ExperimentalCoroutinesApi
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

        Log.i(TAG, "onNewToken: $token")

        sendRegistrationToServer(token)
    }


    private fun handleNow(notification: RemoteMessage.Notification?, data: Map<String, String>?) {
        if (notification == null || data == null || data.isEmpty()) {
            return
        }

        val contentType = data["content_type"]
        val contentId = data["content_id"] ?: return

        val msgType = RemoteMessageType.fromString(contentType) ?: return

        val intent = createIntent(msgType, contentId) ?: return

        val builder = NotificationCompat.Builder(
                applicationContext,
                getString(news_notification_channel_id))
                .setSmallIcon(R.drawable.logo_round)
                .setContentTitle(notification.title)
                .setStyle(NotificationCompat.BigTextStyle()
                        .bigText(notification.body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(intent)

        with(NotificationManagerCompat.from(this)) {
            notify(1, builder.build())
        }
    }

    private fun createIntent(msgType: RemoteMessageType, contentId: String): PendingIntent? {
        val contentType = msgType.toArticleType() ?: return null

        val intent = when (msgType) {
            RemoteMessageType.Story,
            RemoteMessageType.Video,
            RemoteMessageType.Photo,
            RemoteMessageType.Interactive -> ArticleActivity.newIntent(
                    baseContext,
                    Teaser(
                            id = contentId,
                            type = ArticleType.fromString(contentType),
                            title = ""
                    )
            )

            RemoteMessageType.Tag,
            RemoteMessageType.Channel -> ChannelActivity.newIntent(
                    baseContext,
                    ChannelSource(
                            title = contentId,
                            name = "${msgType}_$contentId",
                        path = "",
                        query = "",
                            htmlType = HTML_TYPE_FRAGMENT
                    )
            )
        }

        return TaskStackBuilder.create(this).addNextIntentWithParentStack(intent)
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun sendRegistrationToServer(token: String?) {
        Log.i(TAG, "Sending new token to server: $token")
    }
}
