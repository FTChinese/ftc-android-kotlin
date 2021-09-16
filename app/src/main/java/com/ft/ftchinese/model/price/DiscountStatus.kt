package com.ft.ftchinese.model.price

import com.ft.ftchinese.model.enums.OfferKind

enum class DiscountStatus(val code: String) {
    Active("active"),
    Paused("paused"),
    Cancelled("cancelled");

    companion object {
        private val STRING_TO_ENUM: Map<String, OfferKind> = OfferKind.values().associateBy { it.code }

        @JvmStatic
        fun fromString(symbol: String?): OfferKind? {
            if (symbol == null) {
                return null
            }
            return STRING_TO_ENUM[symbol]
        }
    }
}
