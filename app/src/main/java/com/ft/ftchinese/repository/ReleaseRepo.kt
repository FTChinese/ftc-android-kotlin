package com.ft.ftchinese.repository

import com.ft.ftchinese.R
import com.ft.ftchinese.model.AppRelease
import com.ft.ftchinese.model.fetch.APIError
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.fetch.HttpResp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ReleaseRepo {

    fun getLatest(): HttpResp<AppRelease> {
        return Fetch()
            .setAppId()
            .setApiKey()
            .get(Endpoint.latestRelease)
            .endJson(withRaw = true)
    }

    suspend fun asyncGetLatest(): FetchResult<AppRelease> {
        try {
            val resp = withContext(Dispatchers.IO) {
                getLatest()
            }

            return if (resp.body == null) {
                FetchResult.LocalizedError(R.string.release_not_found)
            } else {
                return FetchResult.Success(resp.body)
            }
        } catch (e: APIError) {
            return if (e.statusCode == 404) {
                FetchResult.LocalizedError(R.string.release_not_found)
            } else {
                FetchResult.fromApi(e)
            }
        } catch (e: Exception) {
            return FetchResult.fromException(e)
        }
    }
}
