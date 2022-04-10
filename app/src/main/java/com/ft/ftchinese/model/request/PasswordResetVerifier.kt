package com.ft.ftchinese.model.request

import kotlinx.serialization.Serializable

// The verification parameters for password reset session.
@Serializable
data class PasswordResetVerifier(
    val email: String,
    val code: String
)
