package com.ft.ftchinese.model.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Used to build query parameter of the url to collect buyer
// info.
@Serializable
enum class PurchaseAction(val symbol: String) {
    @SerialName("buy")
    BUY("buy"),
    @SerialName("renew")
    RENEW("renew"),
    @SerialName("winback")
    WIN_BACK("winback");

    override fun toString(): String {
        return symbol
    }

    companion object {
        private val stringToEnum: Map<String, PurchaseAction> = values().associateBy {
            it.symbol
        }

        @JvmStatic
        fun fromString(symbol: String?): PurchaseAction? {
            return if (symbol == null) null else stringToEnum[symbol]
        }
    }
}
