package com.ft.ftchinese.model.request

import com.ft.ftchinese.model.fetch.json

// HTTP request to set/update mobile number.
data class MobileVerificationParams(
    val mobile: String,
    val code: String,
) {
    fun toJsonString(): String {
        return json.toJsonString(this)
    }
}
