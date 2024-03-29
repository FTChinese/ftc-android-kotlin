package com.ft.ftchinese.ui.repo

import com.ft.ftchinese.model.enums.ApiMode
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.paywall.*
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.repository.ApiConfig
import com.ft.ftchinese.repository.PaywallClient
import com.ft.ftchinese.store.FileStore
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

        return CartItemFtc.newInstance(price, m)
    }

    fun stripeCheckoutItem(
        priceId: String,
        trialId: String?,
        couponId: String?,
        m: Membership
    ): CartItemStripe? {
        val pwItem = stripeItems[priceId] ?: return null

        val coupon = couponId?.let { id ->
            pwItem.coupons.find { it.id == id }
        }

        return CartItemStripe(
            intent = CheckoutIntent.ofStripe(
                source = m,
                target = pwItem.price,
                hasCoupon = coupon != null
            ),
            recurring = pwItem.price,
            trial = trialId?.let { id ->
                stripeItems[id]?.price
            },
            coupon = coupon
        )
    }

    private fun updateCache(pw: Paywall) {
        cached = pw
        stripeItems = pw
            .stripe.associateBy {
                it.price.id
            }
    }

    suspend fun fromFileCache(mode: ApiMode, cache: FileStore): Paywall? {
        val pw = cache.asyncLoadPaywall(mode)
        if (pw != null) {
            updateCache(pw)
        }

        return pw
    }

    suspend fun fromServer(
        api: ApiConfig,
        scope: CoroutineScope,
        cache: FileStore
    ): FetchResult<Paywall> {
        return when (val result = PaywallClient.asyncRetrieve(api)) {
            is FetchResult.LocalizedError -> result
            is FetchResult.TextError -> result
            is FetchResult.Success -> {
                val (paywall, raw) = result.data
                if (raw.isNotBlank()) {
                    scope.launch(Dispatchers.IO) {
                        cache.savePaywall(api.mode, raw)
                    }
                }

                updateCache(paywall)
                FetchResult.Success(paywall)
            }
        }
    }
}
