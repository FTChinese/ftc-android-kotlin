package com.ft.ftchinese.model.request

import com.ft.ftchinese.model.fetch.json

data class WxLinkParams(
    val ftcId: String
) {
    fun toJsonString(): String {
        return json.toJsonString(this)
    }
}
