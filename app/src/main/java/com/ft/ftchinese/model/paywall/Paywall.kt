package com.ft.ftchinese.model.paywall

import com.ft.ftchinese.model.price.Edition
import com.ft.ftchinese.model.price.Price

data class Paywall(
    val banner: Banner,
    val promo: Promo,
    val products: List<Product>,
    val liveMode: Boolean = true,
) {
    val activePrices: List<Price>
        get() = products.flatMap { it.prices }

    fun findPrice(e: Edition): Price? {
        return products
            .find { it.tier == e.tier }
            ?.prices
            ?.find { it.cycle == e.cycle }
    }

    fun findPrice(id: String): Price? {
        return activePrices.find { it.id == id }
    }
}


