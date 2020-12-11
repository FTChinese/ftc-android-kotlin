package com.ft.ftchinese.model.order

import com.ft.ftchinese.R

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

    val stringRes: Int
        get() = when (this) {
            Active -> R.string.sub_status_active
            Incomplete -> R.string.sub_status_incomplete
            IncompleteExpired -> R.string.sub_status_incomplete_expired
            Trialing -> R.string.sub_status_trialing
            PastDue -> R.string.sub_status_past_due
            Canceled -> R.string.sub_status_cancled
            Unpaid -> R.string.sub_status_unpaid
        }

    override fun toString(): String {
        return symbol
    }

    // Status in an invalid final state. If membership is in one of these state, it's invalid.
    fun isInvalid(): Boolean {
        return arrayOf(IncompleteExpired, PastDue, Canceled, Unpaid).contains(this)
    }

    companion object {
        private val stringToEnum: Map<String, StripeSubStatus> = values().associateBy { it.symbol }

        @JvmStatic
        fun fromString(symbol: String?): StripeSubStatus? {
            return if (symbol == null) null else stringToEnum[symbol]
        }
    }
}
