package com.ft.ftchinese.model.stripesubs

data class StripeCustomer(
    val id: String,
    val ftcId: String,
    val defaultSource: String? = null,
    val defaultPaymentMethod: String? = null,
    val email: String,
    val liveMode: Boolean = true
)

