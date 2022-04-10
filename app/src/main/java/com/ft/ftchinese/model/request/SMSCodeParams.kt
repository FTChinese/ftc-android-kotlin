package com.ft.ftchinese.model.request

import kotlinx.serialization.Serializable

// HTTP request parameter to for SMS code.
@Serializable
data class SMSCodeParams(
    val mobile: String,
)
