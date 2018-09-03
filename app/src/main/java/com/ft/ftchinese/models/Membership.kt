package com.ft.ftchinese.models



data class Membership(
        val type: String = "free",
        val startAt: String?,
        val expireAt: String?
) {
    companion object {
        const val TYPE_FREE = "free"
        const val TYPE_STANDARD = "standard"
    }
}