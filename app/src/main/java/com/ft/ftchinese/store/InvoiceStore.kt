package com.ft.ftchinese.store

import android.content.Context
import androidx.core.content.edit
import com.ft.ftchinese.model.fetch.json
import com.ft.ftchinese.model.ftcsubs.Invoices
import com.ft.ftchinese.model.ftcsubs.PaymentResult
import com.ft.ftchinese.model.invoice.Invoice
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class InvoiceStore private constructor(ctx: Context) : AnkoLogger {
    private val sharedPref = ctx.getSharedPreferences(FILE_NAME_INVOICE, Context.MODE_PRIVATE)

    fun saveInvoices(inv: Invoices) {
        sharedPref.edit(commit = true) {
            clear()
        }

        info("Save invoices")
        sharedPref.edit(commit = true) {
            putString(KEY_PURCHASED, inv.purchased.toJsonString())
            putString(KEY_CARRIED_OVER, inv.carriedOver?.toJsonString())
        }
    }

    fun loadInvoices(): Invoices? {
        info("Loading invoices")
        val purchased = sharedPref.getString(KEY_PURCHASED, null)?.let {
            try {
                json.parse<Invoice>(it)
            } catch (e: Exception) {
                info(e)
                null
            }
        } ?: return null

        return Invoices(
            purchased = purchased,
            carriedOver = sharedPref.getString(KEY_CARRIED_OVER, null)?.let {
                try {
                    json.parse(it)
                } catch (e: Exception) {
                    info(e)
                    null
                }
            }
        )
    }

    private fun loadOrderId(): String? {
        return sharedPref.getString(KEY_PURCHASED, null)?.let {
            try {
                val inv = json.parse<Invoice>(it)
                inv?.orderId
            } catch (e: Exception) {
                null
            }
        }
    }

    fun loadPayResult(): PaymentResult? {
        val pr = sharedPref.getString(KEY_PAYMENT_VERIFIED, null)?.let {
            try {
                json.parse<PaymentResult>(it)
            } catch (e: Exception) {
                null
            }
        }

        if (pr != null) {
            return pr
        }

        // If payment verification result is not found,
        // construct an empty one.
        val orderId = loadOrderId() ?: return null

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

    fun savePayResult(pr: PaymentResult) {
        sharedPref.edit {
            putString(KEY_PAYMENT_VERIFIED, json.toJsonString(pr))
        }
    }

    companion object {
        const val FILE_NAME_INVOICE = "com.ft.ftchinese.latest_invoices"
        const val KEY_PURCHASED = "purchased"
        const val KEY_CARRIED_OVER = "carried_over"
        const val KEY_PAYMENT_VERIFIED = "payment_verified"

        private var instance: InvoiceStore? = null

        @Synchronized fun getInstance(ctx: Context): InvoiceStore {
            if (instance == null) {
                instance = InvoiceStore(ctx)
            }

            return instance!!
        }
    }
}
