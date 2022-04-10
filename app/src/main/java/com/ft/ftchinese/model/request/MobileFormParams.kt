package com.ft.ftchinese.model.request

import kotlinx.serialization.Serializable

// HTTP request to set/update mobile number.
@Serializable
data class MobileFormParams(
    val mobile: String,
    val code: String,
)
