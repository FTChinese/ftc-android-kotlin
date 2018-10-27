package com.ft.ftchinese.models

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat


data class Membership(
        val tier: String,
        val billingCycle: String = BILLING_YEARLY,
        // ISO8601 format. Example: 2019-08-05
        val expireDate: String
) {
    /**
     * Compare expireAt against now.
     */
    val isExpired: Boolean
        get() {
            if (expireDate.isBlank()) return true

            // If expire date is before now, it is expired;
            // otherwise it is still valid.
            return DateTime.parse(expireDate, ISODateTimeFormat.date()).isBeforeNow
        }

    // Determine is renewal button is visible.
    // Only check subscribed user.
    val isRenewable: Boolean
        get() {

            if (expireDate.isBlank()) return false

            val expire = DateTime.parse(expireDate, ISODateTimeFormat.date())

            return when (billingCycle) {
                BILLING_YEARLY -> {
                    return expire.isBefore(DateTime.now().plusYears(1).plusDays(1))
                }
                BILLING_MONTHLY -> {
                    return expire.isBefore(DateTime.now().plusMonths(1).plusDays(1))
                }
                else -> false
            }

        }

    val isPaidMember: Boolean
        get() {
            return tier == Membership.TIER_STANDARD || tier == Membership.TIER_PREMIUM
        }


    // Use the combination of tier and billing cycle to determine the price to show.
    val priceKey: String
        get() = "${tier}_$billingCycle"

    fun extendedExpireDate(cycle: String): String {
        val inst = DateTime.parse(expireDate, ISODateTimeFormat.date())

        val newInst = when (cycle) {
            BILLING_YEARLY -> inst.plusYears(1)
            BILLING_MONTHLY -> inst.plusMonths(1)
            else -> inst
        }


        return ISODateTimeFormat
                .date()
                .print(newInst)
    }

    companion object {
        const val TIER_STANDARD = "standard"
        const val TIER_PREMIUM = "premium"

        const val BILLING_YEARLY = "year"
        const val BILLING_MONTHLY = "month"

        const val PAYMENT_METHOD_ALI = 1
        const val PAYMENT_METHOD_WX = 2
        const val PAYMENT_METHOD_STRIPE = 3
    }

}

