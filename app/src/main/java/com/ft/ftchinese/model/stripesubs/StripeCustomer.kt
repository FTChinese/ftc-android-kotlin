package com.ft.ftchinese.model.stripesubs

import kotlinx.serialization.Serializable

@Serializable
data class StripeCustomer(
    val id: String,
    val ftcId: String,
    val defaultSource: String? = null,
    val defaultPaymentMethod: String? = null,
    val email: String,
    val liveMode: Boolean = true
)

