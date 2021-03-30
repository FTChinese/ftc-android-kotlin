package com.ft.ftchinese.model.price

import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.OfferKind
import com.ft.ftchinese.model.enums.PriceSource
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.fetch.KCycle
import com.ft.ftchinese.model.fetch.KPriceSource
import com.ft.ftchinese.model.fetch.KTier
import com.ft.ftchinese.tracking.GAAction

/**
 * Price contains data of a product's price.
 * It unifies both ftc and Stripe product.
 */
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
    @Deprecated(message = "Use offers array")
    val promotionOffer: Discount = Discount(),
    val offers: List<Discount> = listOf(),
) {

    val edition: Edition
        get() = Edition(
            tier = tier,
            cycle = cycle,
        )

    val namedKey: String
        get() = "${tier}_${cycle}"

    val gaAction: String
        get() = when (tier) {
            Tier.STANDARD -> when (cycle) {
                Cycle.YEAR -> GAAction.BUY_STANDARD_YEAR
                Cycle.MONTH -> GAAction.BUY_STANDARD_MONTH
            }
            Tier.PREMIUM -> GAAction.BUY_PREMIUM
        }

    fun applicableOffer(filters: List<OfferKind>): Discount? {
        if (offers.isEmpty()) {
            return null
        }

        val filtered = offers.filter {
                it.isValid() && filters.contains(it.kind)
            }
            .sortedByDescending {
                it.priceOff
            }

        if (filtered.isNullOrEmpty()) {
            return null
        }

        return filtered[0]
    }
}
