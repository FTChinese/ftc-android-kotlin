package com.ft.ftchinese.ui.subs.catalog

import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.paywall.CartItemFtc

data class CatalogFtcCheckout(
    val priceId: String,
    val cartItem: CartItemFtc,
    val payMethod: PayMethod,
)

object CatalogFtcCheckoutStore {
    private var pending: CatalogFtcCheckout? = null

    fun save(item: CatalogFtcCheckout) {
        pending = item
    }

    fun peek(priceId: String?): CatalogFtcCheckout? {
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
