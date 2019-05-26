package com.ft.ftchinese.models

import android.os.Parcelable
import com.beust.klaxon.Json
import com.ft.ftchinese.util.GAAction
import com.ft.ftchinese.util.KCycle
import com.ft.ftchinese.util.KTier
import kotlinx.android.parcel.Parcelize

/**
 * A pricing plan.
 */
data class Plan(
        @KTier
        val tier: Tier,
        @KCycle
        val cycle: Cycle,
        val listPrice: Double,
        val netPrice: Double,
        val description: String
)

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
                listPrice = 258.00,
                netPrice = 258.00,
                description = "FT中文网 - 年度标准会员"
        ),
        standardMonth = Plan(
                tier = Tier.STANDARD,
                cycle = Cycle.MONTH,
                listPrice = 28.00,
                netPrice = 28.00,
                description = "FT中文网 - 月度标准会员"
        ),
        premiumYear = Plan(
                tier = Tier.PREMIUM,
                cycle = Cycle.YEAR,
                listPrice = 1998.00,
                netPrice = 1998.00,
                description = "FT中文网 - 高端会员"
        )
)

/**
 * PlanPayable can be used to build UI for checkout.
 */
@Parcelize
data class PlanPayable(
        @KTier
        val tier: Tier,
        @KCycle
        val cycle: Cycle,
        val listPrice: Double,
        val netPrice: Double,

        val balance: Double,
        val cycleCount: Int,
        val extraDays: Int,
        val payable: Double,

        var isUpgrade: Boolean = false,
        var isRenew: Boolean = false
): Parcelable {

    fun isPayRequired(): Boolean {
        return payable != 0.0
    }

    // Whether user's account balance is enough to cover
    // upgrade cost.
    fun isDirectUpgrade(): Boolean {
        return isUpgrade && payable > 0.0
    }

    fun getId(): String {
        return "${tier.string()}_${cycle.string()}"
    }

    fun gaAddCartAction(): String {
        return when (tier) {
            Tier.STANDARD -> when (cycle) {
                   Cycle.YEAR -> GAAction.BUY_STANDARD_YEAR
                   Cycle.MONTH -> GAAction.BUY_STANDARD_MONTH
            }
            Tier.PREMIUM -> GAAction.BUY_PREMIUM
        }
    }

    companion object {
        fun fromPlan(p: Plan): PlanPayable {
            return PlanPayable(
                    tier = p.tier,
                    cycle = p.cycle,
                    listPrice = p.listPrice,
                    netPrice = p.netPrice,
                    balance = 0.0,
                    cycleCount = 1,
                    extraDays = 0,
                    payable = p.netPrice,
                    isUpgrade = false,
                    isRenew = false
            )
        }
    }
}
