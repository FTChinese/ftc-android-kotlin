package com.ft.ftchinese.model.price

import android.os.Parcelable
import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.OfferKind
import com.ft.ftchinese.model.enums.PriceSource
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.fetch.KCycle
import com.ft.ftchinese.model.fetch.KPriceSource
import com.ft.ftchinese.model.fetch.KTier
import com.ft.ftchinese.tracking.GAAction
import kotlinx.parcelize.Parcelize

/**
 * Price contains data of a product's price.
 * It unifies both ftc and Stripe product.
 */
@Parcelize
data class Price(
    val id: String,
    @KTier
    val tier: Tier,
    @KCycle
    val cycle: Cycle,
    val active: Boolean = true,
    val currency: String = "cny",
    val description: String? = null,
    val liveMode: Boolean,
    val nickname: String? = null,
    val productId: String,
    @KPriceSource
    val source: PriceSource? = null,
    val unitAmount: Double,
    val offers: List<Discount> = listOf(),
) : Parcelable {

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
