package com.ft.ftchinese.model.request

import kotlinx.serialization.Serializable

// User login with mobile number and SMS verification code.
@Serializable
data class MobileAuthParams(
    val mobile: String,
    val code: String,
    val deviceToken: String,
)
