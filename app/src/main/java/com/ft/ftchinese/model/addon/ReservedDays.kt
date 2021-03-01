package com.ft.ftchinese.model.addon

import com.ft.ftchinese.model.enums.Tier
import kotlin.reflect.KProperty

data class ReservedDays(
    val standardAddOn: Int,
    val premiumAddOn: Int,
) {
    fun plus(other: ReservedDays): ReservedDays {
        return ReservedDays(
            standardAddOn = standardAddOn + other.standardAddOn,
            premiumAddOn = premiumAddOn + other.premiumAddOn,
        )
    }

    fun clear(tier: Tier): ReservedDays {
        return when (tier) {
            Tier.STANDARD -> ReservedDays(
                standardAddOn = 0,
                premiumAddOn = premiumAddOn,
            )
            Tier.PREMIUM -> ReservedDays(
                standardAddOn = standardAddOn,
                premiumAddOn = 0,
            )
        }
    }
}
