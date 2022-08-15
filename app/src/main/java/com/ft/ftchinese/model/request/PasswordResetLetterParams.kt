package com.ft.ftchinese.model.request

import kotlinx.serialization.Serializable

@Serializable
data class PasswordResetLetterParams(
    val email: String,
    val useCode: Boolean, // On mobile apps use a code rather than link.
)
