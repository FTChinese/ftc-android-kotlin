package com.ft.ftchinese.model.request

// HTTP request to set/update mobile number.
data class MobileFormParams(
    val mobile: String,
    val code: String,
)
