package com.ft.ftchinese.model.order

enum class StripeSubStatus(val symbol: String) {
    // if the initial payment attempt fails.
    Incomplete("incomplete"),

    // If the first invoice is not paid within 23 hours
    // This is a terminal state, the open invoice will be voided and no further invoices will be generated
    IncompleteExpired("incomplete_expired"),


    Trialing("trialing"),
    Active("active"),

    // when payment to renew it fails
    PastDue("past_due"),

    // when Stripe has exhausted all payment retry attempts.
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
