package com.ft.ftchinese.model.ftcsubs

data class WxPayParams(
    val appId: String,
    val partnerId: String,
    val prepayId: String,
    val timestamp: String,
    val nonce: String,
    val pkg: String,
    val signature: String,
)
