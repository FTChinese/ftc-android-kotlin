package com.ft.ftchinese.model.request

import com.ft.ftchinese.model.fetch.json

// User login with mobile number and SMS verification code.
data class MobileAuthParams(
    val mobile: String,
    val code: String,
    val deviceToken: String,
) {
    fun toJsonString(): String {
        return json.toJsonString(this)
    }
}
