package com.ft.ftchinese.model.request

// The verification parameters for password reset session.
data class PasswordResetVerifier(
    val email: String,
    val code: String
)
