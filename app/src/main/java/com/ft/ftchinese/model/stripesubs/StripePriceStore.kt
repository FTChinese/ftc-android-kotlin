package com.ft.ftchinese.model.stripesubs

object StripePriceStore {
    private var prices = listOf<StripePrice>()

    fun add(p: List<StripePrice>) {
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
}
