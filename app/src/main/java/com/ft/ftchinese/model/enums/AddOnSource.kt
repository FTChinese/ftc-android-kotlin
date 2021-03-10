package com.ft.ftchinese.model.enums

enum class AddOnSource(val symbol: String) {
    CarryOver("carry_over"),
    Compensation("compensation"),
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
