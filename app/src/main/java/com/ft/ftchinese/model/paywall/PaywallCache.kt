package com.ft.ftchinese.model.paywall

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

    fun getFtc(): Paywall {
        return ftc
    }

    fun clear() {
        ftc = defaultPaywall
        stripe = mapOf()
    }
}
