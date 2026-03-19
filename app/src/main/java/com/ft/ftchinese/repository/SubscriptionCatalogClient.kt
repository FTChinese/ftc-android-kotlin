package com.ft.ftchinese.repository

import android.util.Log
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.fetch.HttpResp
import com.ft.ftchinese.model.subscriptioncatalog.SubscriptionCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

object SubscriptionCatalogClient {
    private const val TAG = "SubscriptionCatalog"

    private fun preferredLanguageTag(): String {
        return try {
            Locale.getDefault().toLanguageTag()
        } catch (_: Exception) {
            "zh-CN"
        }
    }

    fun retrieve(
        api: ApiConfig,
        userId: String?,
    ): HttpResp<SubscriptionCatalog> {
        return Fetch()
            .setApiKey()
            .addHeader("Accept-Language", preferredLanguageTag())
            .addHeader("X-Preferred-Language", preferredLanguageTag())
            .apply {
                if (!userId.isNullOrBlank()) {
                    setUserId(userId)
                }
            }
            .get(api.subscriptionCatalog)
            .endJson()
    }

    suspend fun asyncRetrieve(
        api: ApiConfig,
        userId: String?,
    ): FetchResult<SubscriptionCatalog> {
        return try {
            val response = withContext(Dispatchers.IO) {
                retrieve(api, userId)
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
