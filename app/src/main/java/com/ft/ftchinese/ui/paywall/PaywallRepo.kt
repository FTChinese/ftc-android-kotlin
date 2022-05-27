package com.ft.ftchinese.ui.paywall

import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.paywall.*
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.repository.PaywallClient
import com.ft.ftchinese.store.FileCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object PaywallRepo {
    private var cached: Paywall = defaultPaywall
    private var stripeItems: Map<String, StripePaywallItem> = mapOf()

    fun ftcCheckoutItem(priceId: String, m: Membership): CartItemFtc? {
        val price = cached.products.flatMap { it.prices }
            .findLast { it.id == priceId }
            ?: return null

        return price.buildCartItem(m)
    }

    fun stripeCheckoutItem(priceId: String, trialId: String?, m: Membership): CartItemStripe? {
        val pwItem = stripeItems[priceId] ?: return null

        return CartItemStripe(
            intent = pwItem.price.checkoutIntent(m),
            recurring = pwItem.price,
            trial = trialId?.let { id ->
                stripeItems[id]?.price
            },
            coupon = pwItem.getCoupon()
        )
    }

    private fun updateCache(pw: Paywall) {
        cached = pw
        stripeItems = pw
            .stripe.associateBy {
                it.price.id
            }
    }

    suspend fun fromFileCache(isTest: Boolean, cache: FileCache): Paywall? {
        val pw = cache.asyncLoadPaywall(isTest)
        if (pw != null) {
            updateCache(pw)
        }

        return pw
    }

    suspend fun fromServer(isTest: Boolean, scope: CoroutineScope, cache: FileCache): FetchResult<Paywall> {
        return when (val result = PaywallClient.asyncRetrieve(isTest)) {
            is FetchResult.LocalizedError -> result
            is FetchResult.TextError -> result
            is FetchResult.Success -> {
                val (paywall, raw) = result.data
                if (raw.isNotBlank()) {
                    scope.launch(Dispatchers.IO) {
                        cache.savePaywall(isTest, raw)
                    }
                }

                updateCache(paywall)
                FetchResult.Success(paywall)
            }
        }
    }
}
