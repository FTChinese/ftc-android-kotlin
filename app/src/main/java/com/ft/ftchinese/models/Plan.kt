package com.ft.ftchinese.models

const val KEY_STANDARD_YEAR = "standard_year"
const val KEY_STANDARD_MONTH = "standard_month"
const val KEY_PREMIUM_YEAR = "premium_year"

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

class Pricing(private val plans: Map<String, Plan>) {
    fun findPlan(tier: Tier?, cycle: Cycle?): Plan? {

        val key = tierCycleKey(tier, cycle) ?: return null

        return plans[key]
    }

    fun findPlan(key: String?): Plan? {
        return if (key == null) {
            null
        } else {
            plans[key]
        }
    }
}

fun tierCycleKey(tier: Tier?, cycle: Cycle?): String? {
    if (tier == null || cycle == null) {
        return null
    }
    return "${tier.string()}_${cycle.string()}"
}

val pricingPlans = Pricing(mapOf(
        KEY_STANDARD_YEAR to Plan(
                tier = Tier.STANDARD,
                cycle = Cycle.YEAR,
                listPrice = 258.00,
                netPrice = 258.00,
                description = "FT中文网 - 年度标准会员"
        ),
        KEY_STANDARD_MONTH to Plan(
                tier = Tier.STANDARD,
                cycle = Cycle.MONTH,
                listPrice = 28.00,
                netPrice = 28.00,
                description = "FT中文网 - 月度标准会员"
        ),
        KEY_PREMIUM_YEAR to Plan(
                tier = Tier.PREMIUM,
                cycle = Cycle.YEAR,
                listPrice = 1998.00,
                netPrice = 1998.00,
                description = "FT中文网 - 高端会员"
        )
))