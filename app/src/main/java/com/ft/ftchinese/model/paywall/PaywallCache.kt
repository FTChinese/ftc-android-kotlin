package com.ft.ftchinese.model.paywall

object PaywallCache {
    private var paywall = defaultPaywall

    fun update(p: Paywall) {
        paywall = p
    }

    fun get(): Paywall {
        return paywall
    }

    fun clear() {
        paywall = defaultPaywall
    }
}
