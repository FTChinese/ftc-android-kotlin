package com.ft.ftchinese.model.reader

import kotlinx.serialization.Serializable

// After the password reset code is verified to be valid,
// server send back a token. Submit this token together with
// new password.
@Serializable
data class PwResetBearer(
    val email: String,
    val token: String
)


