package com.ft.ftchinese.repository

import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.fetch.HttpResp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ArticleClient {

    fun crawlFile(url: String): HttpResp<String> {
        return Fetch()
            .get(url)
            .endText()
    }

    suspend fun asyncCrawlFile(url: String): FetchResult<String> {
        return try {
            val resp = withContext(Dispatchers.IO) {
                crawlFile(url)
            }

            if (resp.body.isNullOrBlank()) {
                FetchResult.loadingFailed
            } else {
                FetchResult.Success(resp.body)
            }
        } catch (e: Exception) {
            FetchResult.fromException(e)
        }
    }
}
