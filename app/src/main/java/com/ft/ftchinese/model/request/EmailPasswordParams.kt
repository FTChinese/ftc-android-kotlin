package com.ft.ftchinese.model.request

import kotlinx.serialization.Serializable

@Serializable
data class EmailPasswordParams(
    val email: String,
    val password: String,
)
