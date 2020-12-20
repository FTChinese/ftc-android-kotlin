package com.ft.ftchinese.model.subscription

// Format for version prior to v3.5.4.
//data class StripeCustomer(
//        val id: String = "", // Deprecated. Stripe id for backward compatible.
//        val ftcId: String,
//        val unionId: String?,
//        val stripeId: String,
//        val userName: String?,
//        val email: String
//)

data class StripeCustomer(
    val id: String,
    val ftcId: String,
    val defaultSource: String? = null,
    val defaultPaymentMethod: String? = null,
    val email: String,
    val liveMode: Boolean = true
)

data class StripeSetupIntent(
    val clientSecret: String
)
