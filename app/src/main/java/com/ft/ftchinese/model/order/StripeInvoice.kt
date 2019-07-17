package com.ft.ftchinese.model.order

import com.ft.ftchinese.util.KDateTime
import com.ft.ftchinese.util.KPaymentIntentStatus
import org.threeten.bp.ZonedDateTime

data class StripeInvoice(
        @KDateTime
        val created: ZonedDateTime,
        val currency: String,
        val hostedInvoiceUrl: String? = null,
        val invoicePdf: String? = null,
        val number: String,
        val paid: Boolean,
        @KPaymentIntentStatus
        val paymentIntentStatus: PaymentIntentStatus,
        val receiptNumber: String,
        val status: String
)

