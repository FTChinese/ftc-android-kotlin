package com.ft.ftchinese.model.subscription

import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.Edition
import com.ft.ftchinese.model.enums.PriceSource
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.fetch.KCycle
import com.ft.ftchinese.model.fetch.KPriceSource
import com.ft.ftchinese.model.fetch.KTier

data class Price(
    val id: String,
    @KTier
    val tier: Tier,
    @KCycle
    val cycle: Cycle,
    val active: Boolean = true,
    val currency: String = "cny",
    val liveMode: Boolean,
    val nickname: String? = null,
    val productId: String,
    @KPriceSource
    val source: PriceSource,
    val unitAmount: Double,
    val promotionOffer: Discount = Discount(),
) {

    val edition: Edition
        get() = Edition(
            tier = tier,
            cycle = cycle,
        )
}
