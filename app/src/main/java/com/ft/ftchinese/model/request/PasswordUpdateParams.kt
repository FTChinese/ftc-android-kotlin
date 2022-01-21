package com.ft.ftchinese.model.request

data class PasswordUpdateParams(
    val currentPassword: String,
    val newPassword: String
)
