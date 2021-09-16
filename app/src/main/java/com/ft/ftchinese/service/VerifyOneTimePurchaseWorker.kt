package com.ft.ftchinese.service

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.ft.ftchinese.model.ftcsubs.PaymentResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.repository.FtcPayClient
import com.ft.ftchinese.store.InvoiceStore
import com.ft.ftchinese.store.PaymentManager
import com.ft.ftchinese.store.SessionManager
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class VerifyOneTimePurchaseWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams), AnkoLogger {

    private val ctx = appContext

    override fun doWork(): Result {
        val account = SessionManager
            .getInstance(ctx)
            .loadAccount(raw = true)
            ?: return Result.success()

        return verifyFtcPay(account)
    }

    // Compatibility.
    private fun loadPayResult(): PaymentResult? {
        val invoiceStore = InvoiceStore.getInstance(ctx)
        val pr = invoiceStore.loadPayResult()

        if (pr != null) {
            return pr
        }

        val orderId = PaymentManager.getInstance(ctx)
            .loadOrderId() ?: return null

        return PaymentResult(
            paymentState = "",
            paymentStateDesc = "",
            totalFee = 0,
            transactionId = "",
            ftcOrderId = orderId,
            paidAt = null,
            payMethod = null,
        )
    }

    private fun verifyFtcPay(account: Account): Result {

        val pr = loadPayResult() ?: return Result.failure()

        if (pr.isVerified()) {
            info("Order already paid. Stop verification")
            return Result.success()
        }

        if (pr.ftcOrderId.isEmpty()) {
            info("Order id not found")
            return Result.success()
        }

        try {
            val result = FtcPayClient.verifyOrder(account, pr.ftcOrderId) ?: return Result.failure()
            info(result)

            InvoiceStore.getInstance(ctx).savePayResult(result.payment)

            if (!result.payment.isVerified()) {
                return Result.success()
            }

            SessionManager.getInstance(ctx).saveMembership(result.membership)

            return Result.success()
        } catch (e: Exception) {
            info(e)
            return Result.failure()
        }
    }
}
