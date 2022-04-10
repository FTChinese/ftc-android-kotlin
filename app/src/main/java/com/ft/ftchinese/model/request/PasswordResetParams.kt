package com.ft.ftchinese.model.request

import kotlinx.serialization.Serializable

// Used to reset password.
@Serializable
data class PasswordResetParams(
    val token: String,
    val password: String
)
