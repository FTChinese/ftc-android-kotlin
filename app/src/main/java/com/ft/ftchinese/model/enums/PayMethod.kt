package com.ft.ftchinese.model.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PayMethod(val symbol: String) {
    @SerialName("alipay")
    ALIPAY("alipay"),
    @SerialName("wechat")
    WXPAY("wechat"),
    @SerialName("stripe")
    STRIPE("stripe"),
    @SerialName("apple")
    APPLE("apple"),
    @SerialName("b2b")
    B2B("b2b");

    override fun toString(): String {
        return symbol
    }

    companion object {

        private val stringToEnum: Map<String, PayMethod> = values().associateBy { it.symbol }

        @JvmStatic
        fun fromString(symbol: String?): PayMethod? {
            return stringToEnum[symbol]
        }
    }
}
