package com.ft.ftchinese.model.subscription

import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.PriceSource
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.fetch.KCycle
import com.ft.ftchinese.model.fetch.KPriceSource
import com.ft.ftchinese.model.fetch.KTier
import com.ft.ftchinese.model.ui.PriceParams

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
    val originalPriceParams: PriceParams
        get() = PriceParams(
            currency = currency,
            amount = unitAmount,
            tier = tier,
            cycle = cycle,
        )

    val payablePriceParams: PriceParams
        get() = PriceParams(
            currency = currency,
            amount = unitAmount - (promotionOffer.priceOff ?: 0.0),
            tier = tier,
            cycle = cycle
        )
}
