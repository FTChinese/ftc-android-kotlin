package com.ft.ftchinese.model.request

// Used to reset password.
data class PasswordResetParams(
    val token: String,
    val password: String
)
