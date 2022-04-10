package com.ft.ftchinese.model.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AddOnSource(val symbol: String) {
    @SerialName("carry_over")
    CarryOver("carry_over"),
    @SerialName("compensation")
    Compensation("compensation"),
    @SerialName("user_purchase")
    UserPurchase("user_purchase");

    override fun toString(): String {
        return symbol
    }

    companion object {
        private val stringToEnum: Map<String, AddOnSource> = values().associateBy { it.symbol }

        @JvmStatic
        fun fromString(symbol: String?): AddOnSource? {
            return stringToEnum[symbol]
        }
    }
}
