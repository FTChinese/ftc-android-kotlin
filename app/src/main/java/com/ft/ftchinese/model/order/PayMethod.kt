package com.ft.ftchinese.model.order

enum class PayMethod(val symbol: String) {
    ALIPAY("alipay"),
    WXPAY("wechat"),
    STRIPE("stripe");

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
