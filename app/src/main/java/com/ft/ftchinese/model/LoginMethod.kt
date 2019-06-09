package com.ft.ftchinese.model

enum class LoginMethod {
    EMAIL,
    WECHAT,
    MOBILE;

    fun string(): String {
        return when (this) {
            EMAIL -> "email"
            WECHAT -> "wechat"
            MOBILE -> "mobile"
        }
    }

    companion object {
        fun fromString(s: String?): LoginMethod? {
            return when (s) {
                "email" -> EMAIL
                "wechat" -> WECHAT
                "mobile" -> MOBILE
                else -> null
            }
        }
    }
}
