package com.ft.ftchinese.viewmodel

/**
 * Existence wraps the result of checking if a value exists.
 * For example after checking against API whether an email exists,
 * you should tell a hosting activity what email you checked, and
 * whether it exists or not.
 */
data class Existence(
        val value: String,
        val found: Boolean
)
