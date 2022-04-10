package com.ft.ftchinese.model.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class OfferKind(val code: String) {
    @SerialName("promotion")
    Promotion("promotion"),
    @SerialName("retention")
    Retention("retention"),
    @SerialName("win_back")
    WinBack("win_back"),
    @SerialName("introductory")
    Introductory("introductory");

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
