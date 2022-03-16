package com.ft.ftchinese.model.paywall

import com.ft.ftchinese.model.stripesubs.StripePrice

object StripePriceStore {
    private var prices = listOf<StripePrice>()

    val isEmpty: Boolean
        get() = prices.isEmpty()

    fun set(p: List<StripePrice>) {
        prices = p
    }

    /**
     * Find a price form a list of stripe prices by id.
     */
    fun find(priceId: String): StripePrice? {
        return prices.find {
            it.id == priceId
        }
    }

    fun select(ids: Array<String>): List<StripePrice> {
        return prices.filter {
            ids.contains(it.id)
        }
    }

    fun checkoutItem(ids: StripePriceIDs): CartItemStripe? {
        return find(ids.recurring)?.let { price ->
            CartItemStripe(
                orderKind = ids.orderKind,
                recurringPrice = price,
                trialPrice = ids.trial?.let {
                    find(it)
                }
            )
        }
    }
}
