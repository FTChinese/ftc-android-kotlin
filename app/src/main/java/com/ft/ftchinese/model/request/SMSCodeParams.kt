package com.ft.ftchinese.model.request

import com.ft.ftchinese.model.fetch.json

// HTTP request parameter to for SMS code.
data class SMSCodeParams(
    val mobile: String,
) {
    fun toJsonString(): String {
        return json.toJsonString(this)
    }
}
