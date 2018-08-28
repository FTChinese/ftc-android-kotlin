package com.ft.ftchinese.models

data class Membership(
        val type: String = "free",
        val startAt: String?,
        val expireAt: String?
)