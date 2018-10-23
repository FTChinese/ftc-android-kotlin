package com.ft.ftchinese.models

import com.ft.ftchinese.R
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat


data class Membership(
        val tier: String = TIER_FREE,
        val billingCycle: String = BILLING_YEARLY,
        // ISO8601 format. Example: 2019-08-05
        val expireDate: String? = null
) {
    /**
     * Compare expireAt against now.
     */
    val isExpired: Boolean
        get() {
            val expire = expireDate ?: return true

            return DateTime.parse(expire, ISODateTimeFormat.dateTimeNoMillis()).isBeforeNow
        }

    val tierResId: Int
        get() = when(tier) {
            TIER_STANDARD -> R.string.member_tier_standard
            TIER_PREMIUM -> R.string.member_tier_premium
            else -> R.string.member_tier_free
        }

    // Use after clicked to buy a membership.
    val priceResId: Int
        get() = when(tier) {
            TIER_PREMIUM -> when (billingCycle) {
                BILLING_MONTHLY -> R.string.price_premium_month
                else -> R.string.price_premium_annual
            }
            else -> when (billingCycle) {
                BILLING_MONTHLY -> R.string.price_standard_month
                else -> R.string.price_standard_annual
            }
        }



    companion object {
        const val TIER_FREE = "free"
        const val TIER_STANDARD = "standard"
        const val TIER_PREMIUM = "premium"

        const val BILLING_YEARLY = "year"
        const val BILLING_MONTHLY = "month"

        const val PAYMENT_METHOD_ALI = 1
        const val PAYMENT_METHOD_WX = 2
        const val PAYMENT_METHOD_STRIPE = 3
    }
}