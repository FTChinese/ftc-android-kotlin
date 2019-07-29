package com.ft.ftchinese.ui.login

/**
 * Data validation state of the login form.
 */
data class LoginFormState (
        val error: Int? = null,
        val isEmailValid: Boolean = false,
        val isPasswordValid: Boolean = false
)
