package com.ft.ftchinese.model.request

// User login with mobile number and SMS verification code.
data class MobileAuthParams(
    val mobile: String,
    val code: String,
    val deviceToken: String,
)
