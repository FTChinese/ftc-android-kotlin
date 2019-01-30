package com.ft.ftchinese.models

enum class LoginMethod {
    EMAIL,
    WECHAT,
    MOBILE;

    override fun toString(): String {
        return when (this) {
            EMAIL -> "email"
            WECHAT -> "wechat"
            MOBILE -> "mobile"
        }
    }
}