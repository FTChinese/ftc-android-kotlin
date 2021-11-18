package com.ft.ftchinese.model.ftcsubs

import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.OfferKind
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.fetch.KCycle
import com.ft.ftchinese.model.fetch.KTier
import com.ft.ftchinese.model.enums.Edition

/**
 * Price contains data of a product's price.
 * It unifies both ftc and Stripe product.
 */
data class Price(
    val id: String,
    @KTier
    override val tier: Tier,
    @KCycle
    override val cycle: Cycle,
    val active: Boolean = true,
    val currency: String = "cny",
    val description: String? = null,
    val liveMode: Boolean,
    val nickname: String? = null,
    val productId: String,
    val unitAmount: Double,
    val stripePriceId: String,
    val offers: List<Discount> = listOf(),
) : Edition(
    tier = tier,
    cycle = cycle,
) {

    fun dailyPrice(): DailyPrice {
        return when (cycle) {
            Cycle.YEAR -> DailyPrice(
                holder = "{{dailyAverageOfYear}}",
                replacer = "${unitAmount.div(365)}"
            )
            Cycle.MONTH -> DailyPrice(
                holder = "{{dailyAverageOfMonth}}",
                replacer = "${unitAmount.div(30)}"
            )
        }
    }

    /**
     * Find out the most applicable discount a membership
     * could enjoy among this price's offers.
     */
    fun applicableOffer(filters: List<OfferKind>): Discount? {
        if (offers.isEmpty()) {
            return null
        }

        // Filter the offers that are in the filters
        // list. If there are multiple ones, use
        // the one with the biggest price off.
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
