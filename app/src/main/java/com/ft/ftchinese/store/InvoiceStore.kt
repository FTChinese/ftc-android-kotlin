package com.ft.ftchinese.store

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.ft.ftchinese.model.enums.PurchaseAction
import com.ft.ftchinese.model.fetch.json
import com.ft.ftchinese.model.ftcsubs.ConfirmationResult
import com.ft.ftchinese.model.ftcsubs.Invoices
import com.ft.ftchinese.model.ftcsubs.Order
import com.ft.ftchinese.model.ftcsubs.PaymentResult
import com.ft.ftchinese.model.invoice.Invoice
import com.ft.ftchinese.model.reader.Membership

/**
 * InvoiceStore saves all related data after an order is paid:
 * * the confirmed order;
 * * the update membership;
 * * the membership prior to purchase;
 * * the order's invoice and an optional invoice caused by carry-over.
 * * the type of purchase action
 */
class InvoiceStore private constructor(ctx: Context) {
    private val sharedPref = ctx.getSharedPreferences(FILE_NAME_INVOICE, Context.MODE_PRIVATE)

    fun save(result: ConfirmationResult) {
        sharedPref.edit(commit = true) {
            clear()

            putString(KEY_CONFIRMED_ORDER, result.order.toJsonString())
            putString(KEY_LATEST_MEMBERSHIP, result.membership.toJsonString())
            putString(KEY_MEMBER_SNAPSHOT, result.snapshot.toJsonString())
            putString(KEY_PURCHASED, result.invoices.purchased.toJsonString())
            putString(KEY_CARRIED_OVER, result.invoices.carriedOver?.toJsonString())
            putString(KEY_PURCHASE_ACTION, result.action.toString())
        }
    }

    fun load(): ConfirmationResult? {
        val order = loadOrder() ?: return null

        val membership = sharedPref.getString(KEY_LATEST_MEMBERSHIP, null)?.let {
            try {
                json.parse<Membership>(it)
            } catch (e: Exception) {
                null
            }
        } ?: return null

        val snapshot = sharedPref.getString(KEY_MEMBER_SNAPSHOT, null)?.let {
            try {
                json.parse<Membership>(it)
            } catch (e: Exception) {
                null
            }
        } ?: return null

        val invoices = loadInvoices() ?: return null
        val action = loadPurchaseAction() ?: return null

        return ConfirmationResult(
            order = order,
            membership = membership,
            snapshot = snapshot,
            invoices = invoices,
            action = action
        )
    }

    // Only for testing
    fun savePurchaseAction(a: PurchaseAction) {
        sharedPref.edit(commit = true) {
            putString(KEY_PURCHASE_ACTION, a.toString())
        }
    }

    fun loadPurchaseAction(): PurchaseAction? {
        return sharedPref.getString(
            KEY_PURCHASE_ACTION,
            null
        )?.let {
            PurchaseAction.fromString(it)
        }
    }

    // Only for testing
    fun saveInvoices(inv: Invoices) {

        Log.i(TAG, "Save invoices")
        sharedPref.edit(commit = true) {
            putString(KEY_PURCHASED, inv.purchased.toJsonString())
            putString(KEY_CARRIED_OVER, inv.carriedOver?.toJsonString())
        }
    }

    fun loadInvoices(): Invoices? {
        Log.i(TAG, "Loading invoices")
        val purchased = sharedPref.getString(KEY_PURCHASED, null)?.let {
            try {
                json.parse<Invoice>(it)
            } catch (e: Exception) {
                Log.i(TAG, "$e")
                null
            }
        } ?: return null

        return Invoices(
            purchased = purchased,
            carriedOver = sharedPref.getString(KEY_CARRIED_OVER, null)?.let {
                try {
                    json.parse(it)
                } catch (e: Exception) {
                    Log.i(TAG, "$e")
                    null
                }
            }
        )
    }

    fun loadOrder(): Order? {
        return sharedPref.getString(
            KEY_CONFIRMED_ORDER,
            null
        )?.let {
            try {
                json.parse<Order>(it)
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
        val order = loadOrder() ?: return null

        return PaymentResult(
            paymentState = "",
            paymentStateDesc = "",
            totalFee = 0,
            transactionId = "",
            ftcOrderId = order.id,
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
        private const val TAG = "InvoiceStore"

        const val FILE_NAME_INVOICE = "com.ft.ftchinese.latest_invoices"
        const val KEY_CONFIRMED_ORDER = "confirmed_order"
        const val KEY_MEMBER_SNAPSHOT = "member_snapshot"
        const val KEY_LATEST_MEMBERSHIP = "latest_membership"
        const val KEY_PURCHASED = "purchased_invoice"
        const val KEY_CARRIED_OVER = "carried_over_invoice"
        const val KEY_PURCHASE_ACTION = "purchase_action"
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
