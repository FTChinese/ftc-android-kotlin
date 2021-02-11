package com.ft.ftchinese.model.enums

enum class PriceSource(val symbol: String) {
    Ftc("ftc"),
    Stripe("stripe");

    override fun toString(): String {
        return symbol
    }

    companion object {
        private val stringToEnum: Map<String, PriceSource> = values().associateBy { it.symbol }

        @JvmStatic
        fun fromString(symbol: String?): PriceSource? {
            return stringToEnum[symbol]
        }
    }
}
