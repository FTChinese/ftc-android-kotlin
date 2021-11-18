package com.ft.ftchinese.model.stripesubs

object StripePriceStore {
    private var prices = listOf<StripePrice>()

    fun add(p: List<StripePrice>) {
        prices = p
    }

    /**
     * Find a price form a list of stripe prices by id.
     */
    fun findById(priceId: String): StripePrice? {
        return prices.find {
            it.id == priceId
        }
    }

    /**
     * Find introductory offer belong to a specified product.
     * Server-side setting should ensure that there is only
     * one introductory price under a specific product.
     */
    fun findIntroductory(productId: String): StripePrice? {
        return prices.find {
            it.product == productId && it.isIntroductory && it.metadata.isValid()
        }
    }
}
