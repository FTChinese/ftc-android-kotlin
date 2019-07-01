package com.ft.ftchinese

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import com.ft.ftchinese.model.ChannelItem
import com.ft.ftchinese.ui.article.ArticleActivity
import com.ft.ftchinese.ui.article.EXTRA_CHANNEL_ITEM
import com.ft.ftchinese.ui.article.EXTRA_USE_JSON
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class NewsMessagingService : FirebaseMessagingService(), AnkoLogger {
    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        super.onMessageReceived(remoteMessage)

        info("From: ${remoteMessage?.from}")

        remoteMessage?.data?.isNotEmpty()?.let {
            info("Message data payload: " + remoteMessage.data)
            handleNow()
        }

        remoteMessage?.notification?.let {
            info("Message Notification Body: ${it.body}")
        }

    }

    override fun onNewToken(token: String?) {
        super.onNewToken(token)

        info("onNewToken: $token")

        sendRegistrationToServer(token)
    }

    private fun scheduleJob() {

    }

    private fun handleNow() {

    }

    private fun sendRegistrationToServer(token: String?) {
        info("Sending new token to server: $token")
    }

    private fun sendNotification(messageBody: String) {
        val intent = Intent(this, ArticleActivity::class.java).apply {
            putExtra(EXTRA_CHANNEL_ITEM, ChannelItem(
                    id = "001083331",
                    type = "story",
                    title = "波司登遭做空机构质疑 股价暴跌"
            ))
            putExtra(EXTRA_USE_JSON, true)
        }

        val pendingIntent: PendingIntent? = TaskStackBuilder.create(this).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val builder = NotificationCompat.Builder(this, getString(R.string.news_notification_channel_id))
                .setSmallIcon(R.drawable.logo_round)
                .setContentTitle("波司登遭做空机构质疑 股价暴跌")
//                .setContentText("")
                .setStyle(NotificationCompat.BigTextStyle()
                        .bigText("周一，波司登的股价下跌了24.8%，随后宣布停牌。此前，做空机构Bonitas Research对波司登的收入和利润提出了质疑。"))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
        with(NotificationManagerCompat.from(this)) {
            notify(1, builder.build())
        }
    }
}
