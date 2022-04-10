package com.ft.ftchinese.model.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class LoginMethod {
    @SerialName("email")
    EMAIL,
    @SerialName("wechat")
    WECHAT,
    @SerialName("mobile")
    MOBILE;

    fun string(): String {
        return when (this) {
            EMAIL -> "email"
            WECHAT -> "wechat"
            MOBILE -> "mobile"
        }
    }

    companion object {
        @JvmStatic
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
