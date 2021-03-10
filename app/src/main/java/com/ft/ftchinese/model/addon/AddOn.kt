package com.ft.ftchinese.model.addon

import com.ft.ftchinese.model.enums.Tier

data class AddOn(
    val standardAddOn: Int,
    val premiumAddOn: Int,
) {
    fun plus(other: AddOn): AddOn {
        return AddOn(
            standardAddOn = standardAddOn + other.standardAddOn,
            premiumAddOn = premiumAddOn + other.premiumAddOn,
        )
    }

    fun clear(tier: Tier): AddOn {
        return when (tier) {
            Tier.STANDARD -> AddOn(
                standardAddOn = 0,
                premiumAddOn = premiumAddOn,
            )
            Tier.PREMIUM -> AddOn(
                standardAddOn = standardAddOn,
                premiumAddOn = 0,
            )
        }
    }
}
