package com.ft.ftchinese.service

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.repository.FtcPayClient
import com.ft.ftchinese.store.InvoiceStore
import com.ft.ftchinese.store.SessionManager

class VerifyOneTimePurchaseWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    private val ctx = appContext

    override fun doWork(): Result {
        val account = SessionManager
            .getInstance(ctx)
            .loadAccount(raw = true)
            ?: return Result.success()

        return verifyFtcPay(account)
    }

    private fun verifyFtcPay(account: Account): Result {

        val pr = InvoiceStore
            .getInstance(ctx)
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
            val result = FtcPayClient.verifyOrder(account, pr.ftcOrderId) ?: return Result.failure()
            Log.i(TAG, "$result")

            InvoiceStore.getInstance(ctx).savePayResult(result.payment)

            if (!result.payment.isVerified()) {
                return Result.success()
            }

            SessionManager.getInstance(ctx).saveMembership(result.membership)

            return Result.success()
        } catch (e: Exception) {
            Log.i(TAG, "$e")
            return Result.failure()
        }
    }

    companion object {
        private const val TAG = "VerifyOneTimePurchase"
    }
}
