package com.ft.ftchinese.repository

import android.net.Uri
import android.util.Log
import com.ft.ftchinese.App
import com.ft.ftchinese.model.fetch.APIError
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.fetch.HttpResp
import com.ft.ftchinese.store.WebAccessTokenStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ArticleClient {
    private const val TAG = "ArticleClient"
    private const val ACCESS_TOKEN_COOKIE = "accessToken"

    fun crawlFile(url: String): HttpResp<String> {
        val fetch = Fetch()
            .get(url)

        attachAccessTokenCookie(fetch, url)

        return fetch.endText()
    }

    suspend fun asyncCrawlFile(url: String): FetchResult<String> = try {
        val resp = withContext(Dispatchers.IO) {
            crawlFile(url)
        }

        if (resp.code !in 200 until 300) {
            val bodyText = resp.body ?: ""
            FetchResult.fromApi(
                APIError(
                    message = if (bodyText.isNotBlank()) bodyText else resp.message,
                    statusCode = resp.code
                )
            )
        } else if (resp.body.isNullOrBlank()) {
            FetchResult.loadingFailed
        } else {
            FetchResult.Success(resp.body)
        }
    } catch (e: Exception) {
        FetchResult.fromException(e)
    }

    private fun attachAccessTokenCookie(fetch: Fetch, url: String) {
        val host = Uri.parse(url).host ?: return
        if (!HostConfig.isInternalLink(host) && host != HostConfig.HOST_AI_CHAT) {
            return
        }

        val token = runCatching {
            WebAccessTokenStore.getInstance(App.instance).load()
        }.getOrNull()

        if (token.isNullOrBlank()) {
            Log.i(TAG, "No stored accessToken for article html fetch host=$host")
            return
        }

        fetch.addHeader("Cookie", "$ACCESS_TOKEN_COOKIE=$token")
        Log.i(
            TAG,
            "Attached accessToken cookie for article html fetch host=$host token=${maskToken(token)}"
        )
    }

    private fun maskToken(token: String): String {
        if (token.length <= 12) {
            return token
        }
        return "${token.take(8)}...${token.takeLast(4)}"
    }
}
