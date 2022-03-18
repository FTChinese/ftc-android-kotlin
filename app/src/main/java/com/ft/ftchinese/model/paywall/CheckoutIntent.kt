package com.ft.ftchinese.model.paywall

data class CheckoutIntent(
    val kind: IntentKind,
    val message: String,
)
