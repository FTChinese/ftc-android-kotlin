package com.ft.ftchinese.model.stripesubs

import com.ft.ftchinese.model.fetch.json

data class SubParams(
    val priceId: String,
    val introductoryPriceId: String?,
    val defaultPaymentMethod: String?,
    val coupon: String? = null,
    val idempotency: String? = null
) {
    fun toJsonString(): String {
        return json.toJsonString(this)
    }
}
