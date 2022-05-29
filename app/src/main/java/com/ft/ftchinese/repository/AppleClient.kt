package com.ft.ftchinese.repository

import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.APIError
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.iapsubs.IAPSubsResult
import com.ft.ftchinese.model.reader.Account
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppleClient {
    fun refreshIAP(account: Account): IAPSubsResult? {

        val origTxId = account.membership.appleSubsId ?: throw Exception("Not an Apple subscription")

        return Fetch()
            .patch(Endpoint.refreshIAP(account.isTest, origTxId))
            .noCache()
            .setApiKey()
            .endJson<IAPSubsResult>()
            .body
    }

    suspend fun asyncRefreshIAP(account: Account): FetchResult<IAPSubsResult> {
        try {
            val iapSubs = withContext(Dispatchers.IO) {
                refreshIAP(account)
            }

            return if (iapSubs == null) {
                FetchResult.LocalizedError(R.string.iap_refresh_failed)
            } else {
               FetchResult.Success(iapSubs)
            }
        } catch (e: APIError) {
            return if (e.statusCode == 404) {
                FetchResult.LocalizedError(R.string.loading_failed)
            } else {
                FetchResult.fromApi(e)
            }

        } catch (e: Exception) {
            return FetchResult.fromException(e)
        }
    }
}
