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

        val trialPrice = trial?.let { prices[it] }?.price

        val items = mutableListOf<CartItemStripe>()

        recurring.forEach { id ->
            val pwItem = prices[id]
            if (pwItem != null) {
                // Trial and coupon are mutually exclusive.
                val coupon = if (trialPrice == null) {
                    pwItem.applicableCoupon()
                } else {
                    null
                }

                items.add(
                    CartItemStripe(
                        intent = CheckoutIntent.ofStripe(
                            source = m,
                            target = pwItem.price,
                            hasCoupon = coupon != null
                        ),
                        recurring = pwItem.price,
                        trial = trialPrice,
                        coupon = coupon
                    )
                )
            }
        }

        return items
    }
}
