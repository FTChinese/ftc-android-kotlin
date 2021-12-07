package com.ft.ftchinese.model.paywall

import com.ft.ftchinese.model.enums.Edition
import com.ft.ftchinese.model.ftcsubs.Price

data class Paywall(
    val id: Int,
    val banner: Banner,
    val promo: Promo,
    val products: List<PaywallProduct>,
    val liveMode: Boolean = true,
) {
    private val activePrices: List<Price>
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


