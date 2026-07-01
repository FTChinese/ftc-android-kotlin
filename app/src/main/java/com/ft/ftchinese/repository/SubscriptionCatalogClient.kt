package com.ft.ftchinese.repository

import android.net.Uri
import android.util.Log
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.fetch.HttpResp
import com.ft.ftchinese.model.subscriptioncatalog.SubscriptionCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

object SubscriptionCatalogClient {
    private const val TAG = "SubscriptionCatalog"
    private const val PURCHASE_FLOW_TAG = "FTCPurchaseFlow"

    private fun preferredLanguageTag(): String {
        return try {
            Locale.getDefault().toLanguageTag()
        } catch (_: Exception) {
            "zh-CN"
        }
    }

    private fun catalogUrl(
        api: ApiConfig,
        ccode: String?,
        tier: Tier?,
        offerHint: String?,
        discountFrom: String?,
    ): String {
        if (ccode.isNullOrBlank() && tier == null && offerHint.isNullOrBlank() && discountFrom.isNullOrBlank()) {
            return api.subscriptionCatalog
        }

        val builder = Uri.parse(api.subscriptionCatalog)
            .buildUpon()
            .appendQueryParameter("source", "android")

        if (!ccode.isNullOrBlank()) {
            builder.appendQueryParameter("ccode", ccode)
        }
        if (tier != null) {
            builder.appendQueryParameter("tier", tier.symbol)
        }
        if (!offerHint.isNullOrBlank()) {
            builder.appendQueryParameter("offer", offerHint)
        }
        if (!discountFrom.isNullOrBlank()) {
            builder.appendQueryParameter("from", discountFrom)
        }

        return builder.build().toString()
    }

    fun retrieve(
        api: ApiConfig,
        userId: String?,
        ccode: String? = null,
        tier: Tier? = null,
        offerHint: String? = null,
        discountFrom: String? = null,
    ): HttpResp<SubscriptionCatalog> {
        val url = catalogUrl(api, ccode, tier, offerHint, discountFrom)
        Log.i(
            PURCHASE_FLOW_TAG,
            "catalog_request hasUser=${!userId.isNullOrBlank()} " +
                "tier=${tier?.symbol.orEmpty()} ccode=${ccode.orEmpty()} " +
                "offer=${offerHint.orEmpty()} from=${discountFrom.orEmpty()} url=$url"
        )
        return Fetch()
            .setApiKey()
            .addHeader("Accept-Language", preferredLanguageTag())
            .addHeader("X-Preferred-Language", preferredLanguageTag())
            .apply {
                if (!userId.isNullOrBlank()) {
                    setUserId(userId)
                }
            }
            .get(url)
            .endJson()
    }

    suspend fun asyncRetrieve(
        api: ApiConfig,
        userId: String?,
        ccode: String? = null,
        tier: Tier? = null,
        offerHint: String? = null,
        discountFrom: String? = null,
    ): FetchResult<SubscriptionCatalog> {
        return try {
            val response = withContext(Dispatchers.IO) {
                retrieve(
                    api = api,
                    userId = userId,
                    ccode = ccode,
                    tier = tier,
                    offerHint = offerHint,
                    discountFrom = discountFrom,
                )
            }

            if (response.body == null) {
                FetchResult.loadingFailed
            } else {
                FetchResult.Success(response.body)
            }
        } catch (e: Exception) {
            Log.i(TAG, e.message ?: "subscription catalog failed")
            FetchResult.fromException(e)
        }
    }
}
