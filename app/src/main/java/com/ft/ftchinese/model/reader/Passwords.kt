package com.ft.ftchinese.model.reader

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

data class Passwords(
        val oldPassword: String,
        val password: String
)

data class PasswordResetter(
    val token: String,
    val password: String
)

// The data used to verify a password reset session is valid
data class PwResetVerifier(
    val email: String,
    val code: String
)

// After the password reset code is verified to be valid,
// server send back a token. Submit this token together with
// new password.
@Parcelize
data class PwResetBearer(
    val email: String,
    val token: String
) : Parcelable


