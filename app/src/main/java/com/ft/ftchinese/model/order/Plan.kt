package com.ft.ftchinese.model.order

import android.os.Parcelable
import com.beust.klaxon.Json
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

        val cycleCount: Long, // new
        val currency: String, // new
        val extraDays: Long, // new
        val listPrice: Double,
        val netPrice: Double,
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

class SubsPlans(
        @Json(name = "standard_year")
        val standardYear: Plan,

        @Json(name = "standard_month")
        val standardMonth: Plan,

        @Json(name = "premium_year")
        val premiumYear: Plan
) {
    fun of(tier: Tier, cycle: Cycle): Plan {
        return when (tier) {
            Tier.STANDARD -> when (cycle) {
                Cycle.YEAR -> standardYear
                Cycle.MONTH -> standardMonth
            }
            Tier.PREMIUM -> premiumYear
        }
    }
}

val subsPlans = SubsPlans(
        standardYear = Plan(
                tier = Tier.STANDARD,
                cycle = Cycle.YEAR,
                cycleCount = 1,
                currency = "cny",
                extraDays = 1,
                listPrice = 258.00,
                netPrice = 258.00,
                description = "FT中文网 - 年度标准会员"
        ),
        standardMonth = Plan(
                tier = Tier.STANDARD,
                cycle = Cycle.MONTH,
                cycleCount = 1,
                currency = "cny",
                extraDays = 1,
                listPrice = 28.00,
                netPrice = 28.00,
                description = "FT中文网 - 月度标准会员"
        ),
        premiumYear = Plan(
                tier = Tier.PREMIUM,
                cycle = Cycle.YEAR,
                cycleCount = 1,
                currency = "cny",
                extraDays = 1,
                listPrice = 1998.00,
                netPrice = 1998.00,
                description = "FT中文网 - 高端会员"
        )
)


