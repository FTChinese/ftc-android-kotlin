package com.ft.ftchinese.viewmodel

enum class ControlField {
    Email,
    PasswordResetCode,
    Password,
    ConfirmPassword
}

data class FormState(
    val error: Int? = null,
    val field: ControlField

)
