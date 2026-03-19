package com.ft.ftchinese.ui.repo

import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.subscriptioncatalog.SubscriptionCatalog
import com.ft.ftchinese.repository.ApiConfig
import com.ft.ftchinese.repository.SubscriptionCatalogClient

object SubscriptionCatalogRepo {
    suspend fun fromServer(
        api: ApiConfig,
        userId: String?,
    ): FetchResult<SubscriptionCatalog> {
        return SubscriptionCatalogClient.asyncRetrieve(api, userId)
    }
}
