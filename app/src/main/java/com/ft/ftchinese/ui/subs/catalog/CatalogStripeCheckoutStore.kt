package com.ft.ftchinese.ui.subs.catalog

import com.ft.ftchinese.model.paywall.CartItemStripe

data class CatalogStripeCheckout(
    val priceId: String,
    val cartItem: CartItemStripe,
)

object CatalogStripeCheckoutStore {
    private var pending: CatalogStripeCheckout? = null

    fun save(item: CatalogStripeCheckout) {
        pending = item
    }

    fun peek(priceId: String?): CatalogStripeCheckout? {
        val current = pending ?: return null
        return if (priceId.isNullOrBlank() || current.priceId == priceId) {
            current
        } else {
            null
        }
    }

    fun clear() {
        pending = null
    }
}
