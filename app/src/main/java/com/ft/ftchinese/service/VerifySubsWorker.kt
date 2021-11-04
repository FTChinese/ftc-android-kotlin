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
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.repository.AppleClient
import com.ft.ftchinese.repository.FtcPayClient
import com.ft.ftchinese.repository.StripeClient
import com.ft.ftchinese.store.InvoiceStore
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.member.MemberActivity
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

/**
 * Verify subscription status each time the app launches.
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class VerifySubsWorker(
    appContext: Context,
    workerParams:
    WorkerParameters
): Worker(appContext, workerParams), AnkoLogger {

    private val ctx = appContext

    override fun doWork(): Result {
        info("Run subscription verification work")

        val account = SessionManager
            .getInstance(ctx)
            .loadAccount(raw = true)
            ?: return Result.success()

        // If add-on is being used, ask API to put it expiration date.
        // In such case we know it has nothing to do with Stripe or IAP, and it is not actually expired.
        if (account.membership.shouldUseAddOn) {
            return migrateAddOn(account)
        }

        checkExpiration(account.membership)

       when (account.membership.payMethod) {
            PayMethod.ALIPAY, PayMethod.WXPAY -> {
                info("Verify ftc pay...")
                return verifyFtcPay(account)
            }
            PayMethod.APPLE -> {
                info("Refresh IAP...")
                return refreshIAP(account)
            }
            PayMethod.STRIPE -> {
                info("Refresh Stripe...")
                return refreshStripe(account)
            }
           else -> {
               return Result.success()
           }
        }
    }

    private fun checkExpiration(m: Membership) {
        if (m.autoRenew) {
            return
        }

        val remains = m.remainingDays()
        info("Membership remaining days $remains")

        if (remains == null || remains > 10) {
            return
        }

        val pendingIntent: PendingIntent? = TaskStackBuilder.create(ctx).run {
            addNextIntentWithParentStack(Intent(ctx, MemberActivity::class.java))
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val builder = NotificationCompat.Builder(ctx, ctx.getString(R.string.news_notification_channel_id))
            .setSmallIcon(R.drawable.logo_round)
            .setContentTitle(if (remains > 0) {
                "会员即将到期"
            } else {
                "会员已过期"
            })
            .setContentText(if (remains > 0 ) {
                "您的会员还剩${remains}天到期，别忘记续订哦~~"
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

    // If current membership is expired and there are addons,
    // ask server to migrate addons to extend expiration date.
    private fun migrateAddOn(account: Account): Result {
        try {
            val m = FtcPayClient.useAddOn(account) ?: return Result.retry()
            SessionManager.getInstance(ctx).saveMembership(m)
            return Result.success()
        } catch (e: Exception) {
            info(e)
            return Result.failure()
        }
    }

    // Verify ftc order payment if current account's
    // membership comes from alipay or wechat pay.
    private fun verifyFtcPay(account: Account): Result {

        val invStore = InvoiceStore.getInstance(ctx)

        val pr = invStore
            .loadPayResult()
            ?: return Result.failure()

        if (pr.isVerified()) {
            info("Order already paid. Stop verification")
            return Result.success()
        }

        if (pr.ftcOrderId.isEmpty()) {
            info("Order id not found")
            return Result.success()
        }

        try {
            val result = FtcPayClient
                .verifyOrder(account, pr.ftcOrderId)
                ?: return Result.failure()
            info(result)

            invStore.savePayResult(result.payment)

            if (!result.payment.isVerified()) {
                return Result.success()
            }

            SessionManager
                .getInstance(ctx)
                .saveMembership(result.membership)

            return Result.success()
        } catch (e: Exception) {
            info(e)
            return Result.failure()
        }
    }

    // Refresh Apple in-app purchase in background
    // in case current membership payment method is apple.
    private fun refreshIAP(account: Account): Result {
        try {
            val result = AppleClient
                .refreshIAP(account)
                ?: return Result.failure()

            SessionManager
                .getInstance(ctx)
                .saveMembership(result.membership)

            return Result.success()
        } catch (e: Exception) {
            info(e)
            return Result.failure()
        }
    }

    // Refresh stripe subscription for current user in background.
    private fun refreshStripe(account: Account): Result {
        try {
            val result = StripeClient
                .refreshSub(account)
                ?: return Result.failure()

            SessionManager
                .getInstance(ctx)
                .saveMembership(result.membership)

            return Result.success()
        } catch (e: Exception) {
            info(e)
            return Result.failure()
        }
    }
}
