package com.ft.ftchinese.model.request

import kotlinx.serialization.Serializable

// User login with mobile number and SMS verification code.
@Serializable
data class MobileAuthParams(
    val mobile: String,
    val code: String,
    val deviceToken: String,
)

// HTTP request to set/update mobile number.
@Serializable
data class MobileFormValue(
    val mobile: String,
    val code: String,
)

// A mobile links to existing email account
// when it is logging in for the first time.
@Serializable
data class MobileLinkParams(
    val email: String,
    val password: String,
    val mobile: String,
    val deviceToken: String,
)
