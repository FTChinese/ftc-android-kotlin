package com.ft.ftchinese.models

import com.ft.ftchinese.util.KCycle
import com.ft.ftchinese.util.KDate
import com.ft.ftchinese.util.KTier
import org.threeten.bp.LocalDate

data class Membership(
        @KTier
        val tier: Tier? = null,
        @KCycle
        val cycle: Cycle? = null,
        // ISO8601 format. Example: 2019-08-05
        @KDate
        val expireDate: LocalDate? = null
) {
    /**
     * Check if membership is expired.
     * @return true if expireDate is before now, or membership does not exist.
     */
    val isExpired: Boolean
        get() = expireDate
                    ?.isBefore(LocalDate.now())
                    ?: true


    // Determine is renewal button is visible.
    // Only check subscribed user.
    val isRenewable: Boolean
        get() {

            if (expireDate == null || cycle == null) return false

            // Add one day more for leniency.
            return expireDate
                    .isBefore(cycle.endDate(LocalDate.now()).plusDays(1))
        }

    // Check weather user is a member.
    // No check for expiration time.
    val isPaidMember: Boolean
        get() {
            return tier == Tier.STANDARD || tier == Tier.PREMIUM
        }


    // Use the combination of tier and billing cycle to uniquely identify this membership.
    // It is used as key to retrieve a price;
    // It is also used as the ITEM_ID for firebase's ADD_TO_CART event.
    val key: String
        get() = tierCycleKey(tier, cycle) ?: ""

//    val price: Double?
//        get() = prices[id]

    // Compare expireDate against another instance.
    // Pick whichever is later.
    fun isNewer(m: Membership): Boolean {
        if (expireDate == null && m.expireDate == null) {
            return false
        }

        if (m.expireDate == null) {
            return true
        }

        if (expireDate == null) {
            return false
        }

        return expireDate.isAfter(m.expireDate)
    }
}

