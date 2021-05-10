package com.ft.ftchinese.model.request

import com.ft.ftchinese.model.fetch.json

data class WxAuthParams(
    val code: String
) {
    fun toJsonString(): String {
        return json.toJsonString(this)
    }
}
