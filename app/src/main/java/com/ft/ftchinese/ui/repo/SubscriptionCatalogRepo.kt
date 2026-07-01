package com.ft.ftchinese.ui.repo

import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.subscriptioncatalog.SubscriptionCatalog
import com.ft.ftchinese.repository.ApiConfig
import com.ft.ftchinese.repository.SubscriptionCatalogClient

object SubscriptionCatalogRepo {
    suspend fun fromServer(
        api: ApiConfig,
        userId: String?,
        ccode: String? = null,
        tier: Tier? = null,
        offerHint: String? = null,
        discountFrom: String? = null,
    ): FetchResult<SubscriptionCatalog> {
        return SubscriptionCatalogClient.asyncRetrieve(
            api = api,
            userId = userId,
            ccode = ccode,
            tier = tier,
            offerHint = offerHint,
            discountFrom = discountFrom,
        )
    }
}
