package com.ft.ftchinese.ui.account

data class UpdateFormState (
        val emailError: Int? = null,
        val nameError: Int? = null,
        val passwordError: Int? = null,
        val isDataValid: Boolean = false
)
