package com.ft.ftchinese.model.order

import com.beust.klaxon.Json
import com.stripe.android.model.PaymentIntent

data class StripeInvoice(
        val id: String,
        val amountDue: Int,
        val amountPaid: Int,
        val amountRemaining: Int,
        val created: Long,
        val currency: String,
        val customer: String,
        val dueDate: Long? = null,
        @Json(name = "invoice_pdf")
        val invoicePdf: String? = null,
        val paid: Boolean,
        @Json(name = "payment_intent")
        val paymentIntent: PaymentIntent,
        @Json(name = "receipt_number")
        val receiptNumber: String,
        val status: String,
        val total: Int
)
