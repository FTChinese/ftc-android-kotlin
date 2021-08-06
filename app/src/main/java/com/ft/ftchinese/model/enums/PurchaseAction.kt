package com.ft.ftchinese.model.enums

// Used to build query parameter of the url to collect buyer
// info.
enum class PurchaseAction(val symbol: String) {
    BUY("buy"),
    RENEW("renew"),
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
