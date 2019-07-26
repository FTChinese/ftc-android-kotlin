package com.ft.ftchinese.service

import android.app.AlarmManager
import android.app.IntentService
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.TestActivity
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import java.util.concurrent.TimeUnit

class PollService: IntentService("PollService"), AnkoLogger {

    override fun onHandleIntent(intent: Intent?) {
        info("Received an intent: $intent")

        if (!isNetworkAvailableAndConnected()) {
            info("No networkd")
            return
        }

        val i = TestActivity.newIntent(this)
        val pi = PendingIntent.getActivity(this, 0, i, 0)

        val notification = NotificationCompat.Builder(this, getString(R.string.news_notification_channel_id))
                .setSmallIcon(R.drawable.logo_round)
                .setContentTitle("波司登遭做空机构质疑 股价暴跌")
//                .setContentText("")
                .setStyle(NotificationCompat.BigTextStyle()
                        .bigText("周一，波司登的股价下跌了24.8%，随后宣布停牌。此前，做空机构Bonitas Research对波司登的收入和利润提出了质疑。"))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build()

        NotificationManagerCompat.from(this)
                .notify(0, notification)

        sendBroadcast(Intent(ACTION_SHOW_NOTIFICATION))
    }

    private fun isNetworkAvailableAndConnected(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val isNetworkAvailable = cm.activeNetwork != null
        return isNetworkAvailable && cm.activeNetworkInfo.isConnected
    }

    companion object {

        private val POLL_INTERVAL_MS = TimeUnit.MINUTES.toMillis(1)

        const val ACTION_SHOW_NOTIFICATION = "com.ft.ftchinese.SHOW_NOTIFICATION"

        @JvmStatic
        fun newIntent(context: Context): Intent {
            return Intent(context, PollService::class.java)
        }

        @JvmStatic
        fun setServiceAlarm(context: Context, isOn: Boolean) {
            val i = newIntent(context)
            val pi = PendingIntent.getService(context, 0, i, 0)

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            if (isOn) {
                alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), POLL_INTERVAL_MS, pi)
            } else {
                alarmManager.cancel(pi)
                pi.cancel()
            }
        }

        @JvmStatic
        fun isServiceAlarmOn(context: Context): Boolean {
            val i = newIntent(context)
            val pi = PendingIntent.getService(context, 0, i, PendingIntent.FLAG_NO_CREATE)
            return pi != null
        }
    }
}
