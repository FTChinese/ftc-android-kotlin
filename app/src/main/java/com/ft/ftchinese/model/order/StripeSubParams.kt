package com.ft.ftchinese.model.order

data class StripeSubParams(
        val customer: String,
        val coupon: String? = null,
        val defaultPaymentMethod: String?
)
