package com.ft.ftchinese.model.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class DiscountStatus(val code: String) {
    @SerialName("active")
    Active("active"),
    @SerialName("paused")
    Paused("paused"),
    @SerialName("cancelled")
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
