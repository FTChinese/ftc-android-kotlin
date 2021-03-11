package com.ft.ftchinese.model.ftcsubs

// This is user's payment intent.
data class WxPayIntent(
    val order: Order,
    val params: WxPayParams,
)
