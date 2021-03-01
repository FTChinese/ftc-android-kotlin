package com.ft.ftchinese.model.enums

enum class CarryOverSource(val symbol: String) {
    OneTimeUpgrade("one_time_upgrade"),
    SwitchToStripe("one_time_to_stripe");

    override fun toString(): String {
        return symbol
    }

    companion object {
        private val stringToEnum: Map<String, CarryOverSource> = values().associateBy { it.symbol }

        @JvmStatic
        fun fromString(symbol: String?): CarryOverSource? {
            return stringToEnum[symbol]
        }
    }
}
