package com.ft.ftchinese.model.request

import com.ft.ftchinese.model.fetch.json

// A mobile links to existing email account
// when it is logging in for the first time.
data class MobileLinkParams(
    val email: String,
    val password: String,
    val mobile: String,
    val deviceToken: String,
) {
    fun toJsonString(): String {
        return json.toJsonString(this)
    }
}
