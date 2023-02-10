package com.ft.ftchinese.service

import android.Manifest
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
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
import com.ft.ftchinese.ui.subs.MemberActivity

/**
 * Verify subscription status each time the app launches.
 */
class VerifySubsWorker(
    appContext: Context,
    workerParams:
    WorkerParameters
): Worker(appContext, workerParams) {

    private val ctx = appContext

    override fun doWork(): Result {
        Log.i(TAG, "Run subscription verification work")

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
                Log.i(TAG, "Verify ftc pay...")
                return verifyFtcPay(account)
            }
            PayMethod.APPLE -> {
                Log.i(TAG, "Refresh IAP...")
                return refreshIAP(account)
            }
            PayMethod.STRIPE -> {
                Log.i(TAG, "Refresh Stripe...")
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
        Log.i(TAG, "Membership remaining days $remains")

        if ((remains == null) || (remains > 10)) {
            return
        }

        // See https://developer.android.com/develop/ui/views/notifications/navigation
        // Create an Intent for the activity you want to start.
        val intent = Intent(ctx, MemberActivity::class.java)
        // Create the TaskStackBuilder
        val pendingIntent: PendingIntent? = TaskStackBuilder.create(ctx).run {
            // Add the intent, which inflates the back stack
            addNextIntentWithParentStack(intent)
            // Get the PendingIntent containing the entire back stack
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        // See https://developer.android.com/develop/ui/views/notifications/build-notification
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
            if (ActivityCompat.checkSelfPermission(
                    ctx,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
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
            Log.i(TAG, "$e")
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
            Log.i(TAG, "Order already paid. Stop verification")
            return Result.success()
        }

        if (pr.ftcOrderId.isEmpty()) {
            Log.i(TAG, "Order id not found")
            return Result.success()
        }

        try {
            val result = FtcPayClient
                .verifyOrder(account, pr.ftcOrderId)
                ?: return Result.failure()
            Log.i(TAG, "$result")

            invStore.savePayResult(result.payment)

            if (!result.payment.isVerified()) {
                return Result.success()
            }

            SessionManager
                .getInstance(ctx)
                .saveMembership(result.membership)

            return Result.success()
        } catch (e: Exception) {
            Log.i(TAG, "$e")
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
            Log.i(TAG, "$e")
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
            Log.i(TAG, "$e")
            return Result.failure()
        }
    }

    companion object {
        private const val TAG = "VerifySubsWorker"
    }
}
