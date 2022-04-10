package com.ft.ftchinese.model.enums

import com.ft.ftchinese.R
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class StripeSubStatus(val symbol: String) {
    // if the initial payment attempt fails.
    @SerialName("incomplete")
    Incomplete("incomplete"),

    // If the first invoice is not paid within 23 hours
    // This is a terminal state, the open invoice will be voided and no further invoices will be generated
    @SerialName("incomplete_expired")
    IncompleteExpired("incomplete_expired"),

    @SerialName("trialing")
    Trialing("trialing"),
    @SerialName("active")
    Active("active"),

    // when payment to renew it fails
    @SerialName("past_due")
    PastDue("past_due"),

    // when Stripe has exhausted all payment retry attempts.
    @SerialName("canceled")
    Canceled("canceled"),
    @SerialName("unpaid")
    Unpaid("unpaid");

    val stringRes: Int
        get() = when (this) {
            Active -> R.string.sub_status_active
            Incomplete -> R.string.sub_status_incomplete
            IncompleteExpired -> R.string.sub_status_incomplete_expired
            Trialing -> R.string.sub_status_trialing
            PastDue -> R.string.sub_status_past_due
            Canceled -> R.string.sub_status_canceled
            Unpaid -> R.string.sub_status_unpaid
        }

    override fun toString(): String {
        return symbol
    }

    // Status in an invalid final state. If membership is in one of these state, it's invalid.
    fun isInvalid(): Boolean {
        return arrayOf(IncompleteExpired, PastDue, Unpaid).contains(this)
    }

    companion object {
        private val stringToEnum: Map<String, StripeSubStatus> = values().associateBy { it.symbol }

        @JvmStatic
        fun fromString(symbol: String?): StripeSubStatus? {
            return if (symbol == null) null else stringToEnum[symbol]
        }
    }
}
