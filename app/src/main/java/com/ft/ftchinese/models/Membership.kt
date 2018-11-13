package com.ft.ftchinese.models

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

private val prices = mapOf(
        "standard_year" to 198.00,
        "standard_month" to 28.00,
        "premium_year" to 1998.00
)

data class Membership(
        val tier: String,
        val billingCycle: String = CYCLE_YEAR,
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
                CYCLE_YEAR -> {
                    return expire.isBefore(DateTime.now().plusYears(1).plusDays(1))
                }
                CYCLE_MONTH -> {
                    return expire.isBefore(DateTime.now().plusMonths(1).plusDays(1))
                }
                else -> false
            }

        }

    val isPaidMember: Boolean
        get() {
            return tier == Membership.TIER_STANDARD || tier == Membership.TIER_PREMIUM
        }


    // Use the combination of tier and billing cycle to uniquely identify this membership.
    // It is used as key to retrieve a price;
    // It is also used as the ITEM_ID for firebase's ADD_TO_CART event.
    val id: String
        get() = "${tier}_$billingCycle"

    val price: Double?
        get() = prices[id]

    // Compare expireDate against another instance.
    // Pick whichever is later.
    fun isNewer(m: Membership): Boolean {
        if (expireDate.isBlank() && m.expireDate.isBlank()) {
            return false
        }

        if (m.expireDate.isBlank()) {
            return true
        }

        if (expireDate.isBlank()) {
            return false
        }

        val selfExpire = DateTime.parse(expireDate, ISODateTimeFormat.date())
        val anotherExpire = DateTime.parse(expireDate, ISODateTimeFormat.date())

        return selfExpire.isAfter(anotherExpire)
    }

    fun extendedExpireDate(cycle: String): String {
        val inst = DateTime.parse(expireDate, ISODateTimeFormat.date())

        val newInst = when (cycle) {
            CYCLE_YEAR -> inst.plusYears(1)
            CYCLE_MONTH -> inst.plusMonths(1)
            else -> inst
        }


        return ISODateTimeFormat
                .date()
                .print(newInst)
    }

    companion object {
        const val TIER_STANDARD = "standard"
        const val TIER_PREMIUM = "premium"

        const val CYCLE_YEAR = "year"
        const val CYCLE_MONTH = "month"
    }
}

