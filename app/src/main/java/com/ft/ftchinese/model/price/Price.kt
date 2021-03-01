package com.ft.ftchinese.model.price

import com.ft.ftchinese.model.enums.Cycle
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
    val promotionOffer: Discount = Discount(),
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

    // Turn to CheckoutItem used only by WxPayEntryActivity.
    // Usually you should use CheckoutCounter to calculate it.
    // The WxPayEntryActivity is isolated from the rest of teh app.
    // When user enters that page, we've lost track of what user
    // has selected.
    val checkoutItem: CheckoutItem
        get() = if (promotionOffer.isValid()) {
            CheckoutItem(
                price = this,
                discount = promotionOffer,
            )
        } else {
            CheckoutItem(
                price = this,
            )
        }
}
