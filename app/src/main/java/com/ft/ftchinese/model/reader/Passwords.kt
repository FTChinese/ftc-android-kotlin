package com.ft.ftchinese.model.reader

// Parameter to update password.
data class Passwords(
        val oldPassword: String,
        val password: String
)

// After the password reset code is verified to be valid,
// server send back a token. Submit this token together with
// new password.
data class PwResetBearer(
    val email: String,
    val token: String
)


