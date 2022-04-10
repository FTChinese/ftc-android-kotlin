package com.ft.ftchinese.model.request

import kotlinx.serialization.Serializable

@Serializable
data class PasswordUpdateParams(
    val currentPassword: String,
    val newPassword: String
)
