package com.ft.ftchinese.model.order

import com.ft.ftchinese.util.KPaymentIntentStatus

data class StripePayIntent(
        val clientSecret: String,
        // the state of the payment attempt for this subscription’s invoice
        @KPaymentIntentStatus
        val status: PaymentIntentStatus
)
