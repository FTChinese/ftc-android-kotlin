package com.ft.ftchinese.models

import com.ft.ftchinese.R
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat


data class Membership(
        val tier: String = TIER_FREE,
        val billingCycle: String = BILLING_YEARLY,
        val startAt: String? = null,
        // ISO8601 format. Example: 2019-08-05T07:19:41Z
        val expireAt: String? = null
) {
    /**
     * Compare expireAt against now.
     */
    val isExpired: Boolean
        get() {
            val expire = expireAt ?: return true

            return DateTime.parse(expire, ISODateTimeFormat.dateTimeNoMillis()).isBeforeNow
        }

    val tierResId: Int
        get() = when(tier) {
            TIER_PREMIUM -> R.string.member_type_premium
            else -> R.string.member_type_standard
        }

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

    val price: Int
        get() = when(tier) {
            TIER_PREMIUM -> PRICE_PREMIUM
            else -> PRICE_STANDARD
        }

    // 2019-08-06
    val localizedExpireDate: String?
        get() {
            val expire = expireAt ?: return null
            val dateTime = DateTime.parse(expire, ISODateTimeFormat.dateTimeNoMillis())
            return ISODateTimeFormat.date().print(dateTime)
        }

    companion object {
        const val TIER_FREE = "free"
        const val TIER_STANDARD = "standard"
        const val TIER_PREMIUM = "premium"

        const val BILLING_YEARLY = "year"
        const val BILLING_MONTHLY = "month"

        const val PRICE_STANDARD = 198
        const val PRICE_PREMIUM = 1998

        const val PAYMENT_METHOD_ALI = 1
        const val PAYMENT_METHOD_WX = 2
        const val PAYMENT_METHOD_STRIPE = 3
    }
}