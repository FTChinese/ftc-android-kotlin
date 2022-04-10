package com.ft.ftchinese.model.request

import kotlinx.serialization.Serializable

@Serializable
data class CustomerParams(
    val customer: String,
)
