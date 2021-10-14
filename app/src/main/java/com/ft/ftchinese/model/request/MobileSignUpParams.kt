package com.ft.ftchinese.model.request

import com.ft.ftchinese.model.fetch.json

data class MobileSignUpParams(
    val mobile: String,
    val deviceToken: String,
) {
    fun toJsonString(): String {
        return json.toJsonString(this)
    }
}
