package com.ft.ftchinese.repository

import android.content.Context
import android.util.Log
import com.ft.ftchinese.database.StarredArticle
import com.ft.ftchinese.model.enums.ArticleType
import com.ft.ftchinese.model.fetch.APIError
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.store.WebAccessTokenStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val TAG = "SavedContentClient"
private const val SAVED_CONTENT_PAGE_LIMIT = 1000

@Serializable
data class SavedContentAction(
    val id: String,
    val type: String,
    val action: String,
)

@Serializable
data class SavedContentListStats(
    val total: Int = 0,
    val offset: Int = 0,
    val limit: Int = SAVED_CONTENT_PAGE_LIMIT,
    val returned: Int = 0,
    val missing: Int = 0,
)

@Serializable
data class SavedContentListResponse(
    val stats: SavedContentListStats = SavedContentListStats(),
    val current: List<SavedContentItem> = listOf(),
)

@Serializable
data class SavedContentItem(
    val id: String = "",
    val type: String = "",
    val cheadline: String = "",
    val clongleadbody: String = "",
    val cshortleadbody: String = "",
    val tag: String = "",
    @SerialName("subType")
    val subType: String = "",
    val subtime: String = "",
    val pubdate: String = "",
) {
    fun toStarredArticle(): StarredArticle? {
        if (id.isBlank() || type.isBlank() || cheadline.isBlank()) {
            return null
        }

        return StarredArticle(
            id = id,
            type = SavedContentClient.localTypeFromCloudType(type),
            subType = subType,
            title = cheadline,
            standfirst = clongleadbody.ifBlank { cshortleadbody },
            keywords = tag,
            publishedAt = pubdate,
            starredAt = subtime,
        )
    }
}

object SavedContentClient {

    fun cloudTypeFromLocalType(type: String): String {
        return when (type) {
            ArticleType.Premium.toString() -> ArticleType.Story.toString()
            ArticleType.Gallery.toString() -> "photo"
            else -> type
        }
    }

    fun localTypeFromCloudType(type: String): String {
        return when (type) {
            "photo" -> ArticleType.Gallery.toString()
            else -> type
        }
    }

    fun isSyncableCloudType(type: String): Boolean {
        return when (type) {
            "story",
            "interactive",
            "video",
            "photo",
            "content" -> true
            else -> false
        }
    }

    fun isSyncableLocalType(type: String): Boolean {
        return isSyncableCloudType(cloudTypeFromLocalType(type))
    }

    fun savedContentKey(id: String, type: String): String {
        return "${cloudTypeFromLocalType(type)}:$id"
    }

    fun savedContentKey(article: StarredArticle): String {
        return savedContentKey(article.id, article.type)
    }

    suspend fun asyncFetchAll(
        context: Context,
        userId: String,
        baseUrls: List<String>,
    ): FetchResult<List<SavedContentItem>> {
        return withAccessToken(context) { token ->
            tryBaseUrls(baseUrls) { baseUrl ->
                val items = mutableListOf<SavedContentItem>()
                var offset = 0
                var total = Int.MAX_VALUE

                while (offset < total) {
                    val response = withContext(Dispatchers.IO) {
                        Fetch()
                            .get(Endpoint.savedContentList(baseUrl, userId))
                            .setBearer(token)
                            .addQuery("offset", offset.toString())
                            .addQuery("limit", SAVED_CONTENT_PAGE_LIMIT.toString())
                            .noCache()
                            .endJson<SavedContentListResponse>()
                            .body
                    } ?: return@tryBaseUrls FetchResult.loadingFailed

                    items.addAll(response.current)
                    total = response.stats.total
                    offset += response.stats.limit.coerceAtLeast(1)
                }

                FetchResult.Success(items)
            }
        }
    }

    suspend fun asyncSave(
        context: Context,
        key: String,
        baseUrls: List<String>,
    ): FetchResult<Boolean> {
        return asyncUpdate(context, key, "save", baseUrls)
    }

    suspend fun asyncUnsave(
        context: Context,
        key: String,
        baseUrls: List<String>,
    ): FetchResult<Boolean> {
        return asyncUpdate(context, key, "unsave", baseUrls)
    }

    private suspend fun asyncUpdate(
        context: Context,
        key: String,
        action: String,
        baseUrls: List<String>,
    ): FetchResult<Boolean> {
        val parsed = SavedContentKey.parse(key) ?: return FetchResult.unknownError
        if (!isSyncableCloudType(parsed.type)) {
            return FetchResult.Success(true)
        }

        return withAccessToken(context) { token ->
            tryBaseUrls(baseUrls) { baseUrl ->
                withContext(Dispatchers.IO) {
                    Fetch()
                        .post(Endpoint.saveContent(baseUrl))
                        .setBearer(token)
                        .sendJson(
                            SavedContentAction(
                                id = parsed.id,
                                type = parsed.type,
                                action = action,
                            )
                        )
                        .endJson<Map<String, String>>()
                }

                FetchResult.Success(true)
            }
        }
    }

    private suspend fun <T : Any> tryBaseUrls(
        baseUrls: List<String>,
        block: suspend (String) -> FetchResult<T>,
    ): FetchResult<T> {
        var lastError: FetchResult<Nothing>? = null

        for (baseUrl in normalizeBaseUrls(baseUrls)) {
            val result = try {
                block(baseUrl)
            } catch (e: APIError) {
                FetchResult.fromApi(e)
            } catch (e: Exception) {
                FetchResult.fromException(e)
            }

            when (result) {
                is FetchResult.Success -> return result
                is FetchResult.LocalizedError -> {
                    lastError = result
                    Log.w(TAG, "Saved content sync failed at $baseUrl: $result")
                }
                is FetchResult.TextError -> {
                    lastError = result
                    Log.w(TAG, "Saved content sync failed at $baseUrl: ${result.text}")
                }
            }
        }

        return lastError ?: FetchResult.loadingFailed
    }

    private fun normalizeBaseUrls(baseUrls: List<String>): List<String> {
        return baseUrls
            .map { it.trim().trimEnd('/') }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private suspend fun <T : Any> withAccessToken(
        context: Context,
        block: suspend (String) -> FetchResult<T>,
    ): FetchResult<T> {
        val token = WebAccessTokenStore.getInstance(context).load()
            ?.takeIf { it.isNotBlank() }
            ?: return FetchResult.accountNotFound

        return try {
            block(token)
        } catch (e: APIError) {
            FetchResult.fromApi(e)
        } catch (e: Exception) {
            FetchResult.fromException(e)
        }
    }
}

data class SavedContentKey(
    val type: String,
    val id: String,
) {
    companion object {
        fun parse(value: String): SavedContentKey? {
            val separator = value.indexOf(':')
            if (separator <= 0 || separator == value.lastIndex) {
                return null
            }

            val type = value.substring(0, separator)
            val id = value.substring(separator + 1)
            if (type.isBlank() || id.isBlank()) {
                return null
            }

            return SavedContentKey(type = type, id = id)
        }
    }
}
