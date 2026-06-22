package com.ft.ftchinese.repository

import android.content.Context
import android.util.Base64
import android.util.Log
import com.ft.ftchinese.database.ArticleDb
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.store.SavedContentSyncStore
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.store.WebAccessTokenStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val TAG = "SavedContentSync"
private const val ACCESS_TOKEN_REFRESH_SKEW_SECONDS = 5 * 60L

sealed class SavedContentSyncResult {
    data class Synced(
        val savedRemote: Int,
        val unsavedRemote: Int,
        val insertedLocal: Int,
        val deletedLocal: Int,
        val finalCount: Int,
    ) : SavedContentSyncResult()

    data class Skipped(val reason: String) : SavedContentSyncResult()
    data class Failed(val error: FetchResult<Nothing>) : SavedContentSyncResult()
}

object SavedContentSync {
    private val mutex = Mutex()

    suspend fun sync(context: Context): SavedContentSyncResult = mutex.withLock {
        withContext(Dispatchers.IO) {
            syncLocked(context.applicationContext)
        }
    }

    private suspend fun syncLocked(context: Context): SavedContentSyncResult {
        val sessionManager = SessionManager.getInstance(context)
        val account = sessionManager.loadAccount(raw = true)
            ?: return SavedContentSyncResult.Skipped("not_logged_in")
        val userId = account.id.takeIf { it.isNotBlank() }
            ?: return SavedContentSyncResult.Skipped("missing_user_id")
        refreshAccessTokenIfNeeded(context, sessionManager, account)
        if (WebAccessTokenStore.getInstance(context).load().isNullOrBlank()) {
            return SavedContentSyncResult.Skipped("missing_access_token")
        }
        val baseUrls = savedContentBaseUrls()

        val db = ArticleDb.getInstance(context)
        val store = SavedContentSyncStore.getInstance(context)
        val localOwner = store.localOwner()
        val allowLocalAdditions = localOwner == null || localOwner == userId

        val localArticles = db.starredDao().loadAll()
            .filter { SavedContentClient.isSyncableLocalType(it.type) }
        val localByKey = localArticles.associateBy { SavedContentClient.savedContentKey(it) }
        val localKeys = localByKey.keys

        val syncedKeys = store.syncedKeys(userId)
        val pendingSaves = store.pendingSaveKeys(userId)
        val pendingUnsaves = store.pendingUnsaveKeys(userId)

        val cloudItems = when (val result = SavedContentClient.asyncFetchAll(context, userId, baseUrls)) {
            is FetchResult.Success -> result.data
            is FetchResult.LocalizedError -> return SavedContentSyncResult.Failed(result)
            is FetchResult.TextError -> return SavedContentSyncResult.Failed(result)
        }

        val cloudByKey = cloudItems
            .filter { SavedContentClient.isSyncableCloudType(it.type) }
            .associateBy { SavedContentClient.savedContentKey(it.id, it.type) }
        val cloudKeys = cloudByKey.keys.toMutableSet()

        var unsavedRemote = 0
        for (key in pendingUnsaves) {
            when (val result = SavedContentClient.asyncUnsave(context, key, baseUrls)) {
                is FetchResult.Success -> {
                    cloudKeys.remove(key)
                    unsavedRemote += 1
                }
                is FetchResult.LocalizedError -> return SavedContentSyncResult.Failed(result)
                is FetchResult.TextError -> return SavedContentSyncResult.Failed(result)
            }
        }

        val implicitLocalSaves = if (allowLocalAdditions) {
            localKeys - syncedKeys - pendingUnsaves
        } else {
            emptySet()
        }
        val saveKeys = (pendingSaves + implicitLocalSaves)
            .filter { key -> key in localByKey && key !in pendingUnsaves }
            .toSet()

        var savedRemote = 0
        for (key in saveKeys) {
            when (val result = SavedContentClient.asyncSave(context, key, baseUrls)) {
                is FetchResult.Success -> {
                    cloudKeys.add(key)
                    savedRemote += 1
                }
                is FetchResult.LocalizedError -> return SavedContentSyncResult.Failed(result)
                is FetchResult.TextError -> return SavedContentSyncResult.Failed(result)
            }
        }

        var insertedLocal = 0
        for ((key, item) in cloudByKey) {
            if (key in pendingUnsaves || key in localByKey) {
                continue
            }

            val article = item.toStarredArticle() ?: continue
            db.starredDao().insertOne(article)
            insertedLocal += 1
        }

        val remoteDeletedKeys = if (allowLocalAdditions) {
            if (syncedKeys.isEmpty()) {
                emptySet()
            } else {
                (syncedKeys - cloudKeys - pendingSaves) intersect localKeys
            }
        } else {
            localKeys - cloudKeys - pendingSaves
        }

        var deletedLocal = 0
        for (key in remoteDeletedKeys) {
            val article = localByKey[key] ?: continue
            db.starredDao().delete(article.id, article.type)
            deletedLocal += 1
        }

        val finalKeys = db.starredDao().loadAll()
            .asSequence()
            .filter { SavedContentClient.isSyncableLocalType(it.type) }
            .map { SavedContentClient.savedContentKey(it) }
            .toSet()

        store.markSynced(userId, finalKeys)

        return SavedContentSyncResult.Synced(
            savedRemote = savedRemote,
            unsavedRemote = unsavedRemote,
            insertedLocal = insertedLocal,
            deletedLocal = deletedLocal,
            finalCount = finalKeys.size,
        )
    }

    private fun refreshAccessTokenIfNeeded(
        context: Context,
        sessionManager: SessionManager,
        account: Account,
    ): Account {
        val token = WebAccessTokenStore.getInstance(context).load()
        if (!token.isNullOrBlank() && !isJwtExpiringSoon(token)) {
            return account
        }

        return runCatching {
            AccountRepo.refresh(account)?.also {
                sessionManager.saveAccount(it)
            }
        }.onSuccess {
            Log.i(TAG, "Refreshed account before saved content sync")
        }.onFailure {
            Log.w(TAG, "Failed to refresh account before saved content sync", it)
        }.getOrNull() ?: account
    }

    private fun savedContentBaseUrls(): List<String> {
        return listOf(
            rootFromApiBase(ApiConfig.ofAuth.baseUrl),
        )
            .map { it.trim().trimEnd('/') }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun rootFromApiBase(baseUrl: String): String {
        return baseUrl
            .trimEnd('/')
            .replace(Regex("/api(?:/(?:v\\d+|sandbox))?$"), "")
    }

    private fun isJwtExpiringSoon(token: String): Boolean {
        val json = decodeJwtPayload(token) ?: return false
        val expiresAt = json.optLong("exp", 0L)
        return expiresAt > 0L &&
                expiresAt <= System.currentTimeMillis() / 1000L + ACCESS_TOKEN_REFRESH_SKEW_SECONDS
    }

    private fun decodeJwtPayload(token: String): JSONObject? {
        val payload = token.split('.').getOrNull(1) ?: return null
        return runCatching {
            val decoded = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
            JSONObject(String(decoded, Charsets.UTF_8))
        }.getOrNull()
    }
}
