package com.ft.ftchinese.model.request

import kotlinx.serialization.Serializable

@Serializable
data class OrderParams(
    val priceId: String,
    val discountId: String?,
)
