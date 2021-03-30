package com.ft.ftchinese.model.enums

enum class OfferKind(val code: String) {
    Promotion("promotion"),
    Retention("retention"),
    WinBack("win_back");

    companion object {
        private val STRING_TO_ENUM: Map<String, OfferKind> = values().associateBy { it.code }

        @JvmStatic
        fun fromString(symbol: String?): OfferKind? {
            if (symbol == null) {
                return null
            }
            return STRING_TO_ENUM[symbol]
        }
    }
}
