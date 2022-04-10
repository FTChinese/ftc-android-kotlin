package com.ft.ftchinese.model.stripesubs

import com.ft.ftchinese.model.fetch.marshaller
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable
data class SubParams(
    val priceId: String,
    val introductoryPriceId: String?,
    val defaultPaymentMethod: String?,
    val coupon: String? = null,
    val idempotency: String? = null
) {
    fun toJsonString(): String {
        return marshaller.encodeToString(this)
    }
}
