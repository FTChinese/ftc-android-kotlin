package com.ft.ftchinese.model.request

data class PasswordResetLetterParams(
    val email: String,
    val useCode: Boolean = true, // On mobile apps use a code rather than link.
)
