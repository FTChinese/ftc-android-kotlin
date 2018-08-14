package com.ft.ftchinese.models

data class Account(
        val email: String,
        val password: String,
        val ip: String? = null
)