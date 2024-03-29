package com.ft.ftchinese.model.request

import kotlinx.serialization.Serializable

@Serializable
data class Credentials(
    val email: String,
    val password: String,
    val deviceToken: String,
)

data class EmailAuthFormVal(
    val email: String,
    val password: String,
)

