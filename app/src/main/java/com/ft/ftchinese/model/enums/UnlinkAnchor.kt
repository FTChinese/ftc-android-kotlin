package com.ft.ftchinese.model.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class UnlinkAnchor(val symbol: String) {
    @SerialName("ftc")
    FTC("ftc"),
    @SerialName("wechat")
    WECHAT("wechat");

    override fun toString(): String {
        return symbol
    }

    companion object {
        private val stringToEnum: Map<String, UnlinkAnchor> = values().associateBy {
            it.symbol
        }

        @JvmStatic
        fun fromString(symbol: String?): UnlinkAnchor? {
            return if (symbol == null) null else stringToEnum[symbol]
        }
    }
}
