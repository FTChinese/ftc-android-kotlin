package com.ft.ftchinese.model.paywall

import com.ft.ftchinese.model.reader.Membership

/**
 * Stripe price ids attached to a specific ftc product.
 */
data class StripePriceIDs(
    val recurring: List<String>,
    val trial: String?,
) {
    fun buildCartItems(
        prices: Map<String, StripePaywallItem>,
        m: Membership
    ): List<CartItemStripe> {
        if (prices.isEmpty()) {
            return listOf()
        }

        val trialItem = trial?.let { prices[it] }

        val items = mutableListOf<CartItemStripe>()

        recurring.forEach { id ->
            val pwItem = prices[id]
            if (pwItem != null) {
                items.add(
                    CartItemStripe(
                        intent = pwItem.price.checkoutIntent(m),
                        recurring = pwItem.price,
                        trial = trialItem?.price,
                        coupon = pwItem.getCoupon()
                    )
                )
            }
        }

        return items
    }
}
