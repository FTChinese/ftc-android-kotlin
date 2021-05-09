package com.ft.ftchinese.model.reader

// After the password reset code is verified to be valid,
// server send back a token. Submit this token together with
// new password.
data class PwResetBearer(
    val email: String,
    val token: String
)


