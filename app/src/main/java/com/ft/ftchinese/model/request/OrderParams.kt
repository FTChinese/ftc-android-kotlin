package com.ft.ftchinese.model.request

import com.ft.ftchinese.model.fetch.json

data class OrderParams(
    val priceId: String,
    val discountId: String?,
) {
    fun toJsonString(): String {
        return json.toJsonString(this)
    }
}
