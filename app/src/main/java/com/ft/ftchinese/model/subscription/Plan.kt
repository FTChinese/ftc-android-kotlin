package com.ft.ftchinese.model.subscription

import android.os.Parcelable
import com.ft.ftchinese.model.order.OrderUsage
import com.ft.ftchinese.util.GAAction
import com.ft.ftchinese.util.KCycle
import com.ft.ftchinese.util.KTier
import kotlinx.android.parcel.Parcelize

/**
 * A plan for a product.
 */
@Parcelize
data class Plan(
        @KTier
        val tier: Tier,
        @KCycle
        val cycle: Cycle,
        val price: Double,
        val amount: Double,
        val currency: String,
        val description: String
) : Parcelable {

    fun getId(): String {
        return "${tier}_$cycle"
    }

    fun currencySymbol(): String {
        return when (currency) {
            "cny" -> "¥"
            "usd" -> "$"
            "gbp" -> "£"
            else -> "¥"
        }
    }

    fun paymentIntent(kind: OrderUsage?): PaymentIntent {
        return PaymentIntent(
                amount = amount,
                currency = currency,
                subscriptionKind = kind,
                plan = this
        )
    }

    fun gaGAAction(): String {
        return when (tier) {
            Tier.STANDARD -> when (cycle) {
                Cycle.YEAR -> GAAction.BUY_STANDARD_YEAR
                Cycle.MONTH -> GAAction.BUY_STANDARD_MONTH
            }
            Tier.PREMIUM -> GAAction.BUY_PREMIUM
        }
    }
}

private val plans = mapOf(
        "standard_year" to Plan(
                tier = Tier.STANDARD,
                cycle = Cycle.YEAR,
                price = 258.00,
                amount = 258.00,
                currency = "cny",

                description = "FT中文网 - 年度标准会员"
        ),
        "standard_month" to Plan(
                tier = Tier.STANDARD,
                cycle = Cycle.MONTH,
                price = 28.00,
                amount = 28.00,
                currency = "cny",

                description = "FT中文网 - 月度标准会员"
        ),
        "premium_year" to Plan(
                tier = Tier.PREMIUM,
                cycle = Cycle.YEAR,
                price = 1998.00,
                amount = 1998.00,
                currency = "cny",

                description = "FT中文网 - 高端会员"
        )
)

fun findPlan(tier: Tier, cycle: Cycle): Plan? {
    return plans["${tier}_$cycle"]
}
