package com.ft.ftchinese.model.subscription

data class StripeCustomer(
        val id: String = "",
        val ftcId: String,
        val unionId: String?,
        val stripeId: String,
        val userName: String?,
        val email: String
)
