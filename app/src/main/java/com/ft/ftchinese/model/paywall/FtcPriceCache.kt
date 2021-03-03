package com.ft.ftchinese.model.paywall

import com.ft.ftchinese.model.price.Edition
import com.ft.ftchinese.model.price.Price

object FtcPriceCache {
    var prices = defaultPaywall.products.flatMap { it.prices }

    fun update(products: List<Product>) {
        prices = products.flatMap { it.prices }
    }

    fun find(id: String): Price? {
        return prices.find { it.id == id }
    }

    fun find(e: Edition): Price? {
        return prices.find {
            it.tier == e.tier && it.cycle == e.cycle
        }
    }
}
