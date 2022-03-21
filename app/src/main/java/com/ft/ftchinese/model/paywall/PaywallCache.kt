package com.ft.ftchinese.model.paywall

import com.ft.ftchinese.model.stripesubs.StripePrice

object PaywallCache {
    private var ftc = defaultPaywall
    private var stripe: MutableMap<String, StripePrice> = mutableMapOf()

    fun setFtc(p: Paywall) {
        ftc = p
    }

    fun setStripe(prices: List<StripePrice>) {
        prices.forEach {
            stripe[it.id] = it
        }
    }

    fun getFtc(): Paywall {
        return ftc
    }

    fun clear() {
        ftc = defaultPaywall
        stripe.clear()
    }
}
