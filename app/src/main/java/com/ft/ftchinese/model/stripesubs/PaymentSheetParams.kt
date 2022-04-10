package com.ft.ftchinese.model.stripesubs

import kotlinx.serialization.Serializable

@Serializable
data class PaymentSheetParams(
    val clientSecret: String,
    val ephemeralKey: String,
    val customerId: String,
    val publishableKey: String,
    val liveMode: Boolean,
)
