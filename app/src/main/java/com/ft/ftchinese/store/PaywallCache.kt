package com.ft.ftchinese.store

import com.ft.ftchinese.model.paywall.Paywall
import com.ft.ftchinese.model.paywall.defaultPaywall
import com.ft.ftchinese.model.stripesubs.StripePrice

object PaywallCache {
    private var ftc = defaultPaywall
    private var stripe: Map<String, StripePrice> = mapOf()

    fun setFtc(p: Paywall) {
        ftc = p
    }

    fun setStripe(p: Map<String, StripePrice>) {
        stripe = p
    }

    fun clear() {
        ftc = defaultPaywall
        stripe = mapOf()
    }
}
