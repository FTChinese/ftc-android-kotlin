package com.ft.ftchinese.model.paywall

import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.stripesubs.StripePrice

/**
 * Stripe price ids attached to a specific ftc product.
 */
data class StripePriceIDs(
    val recurring: List<String>,
    val trial: String?,
) {
    fun listShoppingItems(prices: Map<String, StripePrice>, m: Membership): List<CartItemStripe> {
        if (prices.isEmpty()) {
            return listOf()
        }

        val trialPrice = trial?.let { prices[it] }

        val items = mutableListOf<CartItemStripe>()

        recurring.forEach { id ->
            val price = prices[id]
            if (price != null) {
                items.add(CartItemStripe(
                    intent = price.checkoutIntent(m),
                    recurring = price,
                    trial = trialPrice
                ))
            }
        }

        return items
    }

    companion object {
        fun newInstance(ftcItems: List<CartItemFtc>): StripePriceIDs {
            var trial: String? = null
            val recur = mutableListOf<String>()

            ftcItems.forEach {
                if (it.isIntro) {
                    trial = it.price.stripePriceId
                } else {
                    recur.add(it.price.stripePriceId)
                }
            }

            return StripePriceIDs(
                recurring = recur,
                trial = trial,
            )
        }
    }
}
