package com.ft.ftchinese.model.request

import kotlinx.serialization.Serializable

@Serializable
data class MobileSignUpParams(
    val mobile: String,
    val deviceToken: String,
)
