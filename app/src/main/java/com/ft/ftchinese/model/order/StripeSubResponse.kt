package com.ft.ftchinese.model.order

data class StripeSubResponse(
        val requiresAction: Boolean,
        val paymentIntentClientSecret: String?
)
