package com.ft.ftchinese.model.order

enum class StripeSubStatus(val symbol: String) {
    Incomplete("incomplete"),
    IncompleteExpired("incomplete_expired"),
    Trialing("trialing"),
    Active("active"),
    PastDue("past_due"),
    Canceled("canceled"),
    Unpaid("unpaid");

    override fun toString(): String {
        return symbol
    }

    companion object {
        private val stringToEnum: Map<String, StripeSubStatus> = values().associateBy { it.symbol }

        @JvmStatic
        fun fromString(symbol: String?): StripeSubStatus? {
            return if (symbol == null) null else stringToEnum[symbol]
        }
    }
}
