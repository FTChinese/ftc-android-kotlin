package com.ft.ftchinese.model.paywall

import com.ft.ftchinese.model.stripesubs.StripePrice

@Deprecated("")
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
    @Deprecated("")
    fun find(priceId: String): StripePrice? {
        return prices.find {
            it.id == priceId
        }
    }

    @Deprecated("")
    fun select(ids: Array<String>): List<StripePrice> {
        return prices.filter {
            ids.contains(it.id)
        }
    }
}
