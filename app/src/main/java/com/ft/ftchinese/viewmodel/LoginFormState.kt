package com.ft.ftchinese.viewmodel

/**
 * Data validation state of the login form.
 */
data class LoginFormState (
        val error: Int? = null,
        val isEmailValid: Boolean = false,
        val isPasswordValid: Boolean = false
)
