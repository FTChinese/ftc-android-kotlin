package com.ft.ftchinese.model.request

import kotlinx.serialization.Serializable

// A mobile links to existing email account
// when it is logging in for the first time.
@Serializable
data class MobileLinkParams(
    val email: String,
    val password: String,
    val mobile: String,
    val deviceToken: String,
)
