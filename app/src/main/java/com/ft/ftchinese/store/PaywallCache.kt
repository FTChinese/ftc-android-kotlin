package com.ft.ftchinese.store

import com.ft.ftchinese.model.ftcsubs.Price
import com.ft.ftchinese.model.paywall.Paywall
import com.ft.ftchinese.model.paywall.defaultPaywall
import com.ft.ftchinese.model.stripesubs.StripePrice

object PaywallCache {
    private var ftc = defaultPaywall
    private var stripe: Map<String, StripePrice> = mapOf()

    fun setFtc(p: Paywall) {
        ftc = p
    }

    fun setStripe(prices: List<StripePrice>) {
        stripe = prices.associateBy { it.id }
    }

    fun findFtcPrice(id: String): Price? {
        return ftc.products.flatMap {
            it.prices
        }.find {
            it.id == id
        }
    }

    fun findStripePrice(id: String): StripePrice? {
        return stripe[id]
    }

    fun clear() {
        ftc = defaultPaywall
        stripe = mapOf()
    }
}
