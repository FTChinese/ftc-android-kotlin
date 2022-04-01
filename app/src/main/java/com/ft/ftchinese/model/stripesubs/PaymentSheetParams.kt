package com.ft.ftchinese.model.stripesubs

data class PaymentSheetParams(
    val clientSecret: String,
    val ephemeralKey: String,
    val customerId: String,
    val publishableKey: String,
    val liveMode: Boolean,
)
