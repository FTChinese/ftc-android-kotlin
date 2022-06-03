package com.ft.ftchinese.model.request

import kotlinx.serialization.Serializable

@Serializable
data class PaymentMethodParams(
    val defaultPaymentMethod: String,
)
