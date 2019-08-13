package com.ft.ftchinese.model.reader

enum class UnlinkAnchor {
    FTC,
    WECHAT;

    fun string(): String {
        return when (this) {
            FTC -> "ftc"
            WECHAT -> "wechat"
        }
    }

    companion object {
        @JvmStatic
        fun fromString(s: String?): UnlinkAnchor? {
            return when (s) {
                "ftc" -> FTC
                "wechat" -> WECHAT
                else -> null
            }
        }
    }
}
