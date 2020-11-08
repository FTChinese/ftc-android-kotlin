package com.ft.ftchinese.service

import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.ft.ftchinese.R
import com.ft.ftchinese.repository.AccountRepo
import com.ft.ftchinese.repository.SubRepo
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.store.lastPaidOrderId
import com.ft.ftchinese.ui.pay.MemberActivity
import org.jetbrains.anko.AnkoLogger

@kotlinx.coroutines.ExperimentalCoroutinesApi
class VerifySubsWorker(appContext: Context, workerParams: WorkerParameters):
    Worker(appContext, workerParams), AnkoLogger {

    private val ctx = appContext

    override fun doWork(): Result {
        val account = SessionManager
            .getInstance(ctx)
            .loadAccount()
            ?: return Result.success()

        val remains = account
            .membership
            .remainingDays()

        if (remains != null && remains <= 10) {
            remindWillExpire(remains)
        }

        if (!account.membership.fromWxOrAli()) {
            return Result.success()
        }

        val orderId = lastPaidOrderId(ctx) ?: return Result.success()

        try {
            val result = SubRepo.verifyPayment(account, orderId) ?: return Result.retry()

            if (!result.isOrderPaid()) {
                return Result.success()
            }

            val updatedAccount = AccountRepo.refresh(account) ?: return Result.retry()

            SessionManager.getInstance(ctx).saveAccount(updatedAccount)
        } catch (e: Exception) {
            return Result.retry()
        }

        return Result.success()
    }

    private fun remindWillExpire(days: Long) {
        val pendingIntent: PendingIntent? = TaskStackBuilder.create(ctx).run {
            addNextIntentWithParentStack(Intent(ctx, MemberActivity::class.java))
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val builder = NotificationCompat.Builder(ctx, ctx.getString(R.string.news_notification_channel_id))
            .setSmallIcon(R.drawable.logo_round)
            .setContentTitle(if (days > 0) {
                "会员即将到期"
            } else {
                "会员已过期"
            })
            .setContentText(if (days > 0 ) {
                "您的会员还剩${days}天到期，别忘记续订哦~~"
            } else {
                ""
            })
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(ctx)) {
            notify(1, builder.build())
        }
    }
}
