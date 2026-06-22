package com.ft.ftchinese.repository

import android.content.Context
import com.ft.ftchinese.database.ArticleDb
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.store.SavedContentSyncStore
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.store.WebAccessTokenStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

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
        val account = SessionManager.getInstance(context).loadAccount(raw = true)
            ?: return SavedContentSyncResult.Skipped("not_logged_in")
        val userId = account.id.takeIf { it.isNotBlank() }
            ?: return SavedContentSyncResult.Skipped("missing_user_id")
        if (WebAccessTokenStore.getInstance(context).load().isNullOrBlank()) {
            return SavedContentSyncResult.Skipped("missing_access_token")
        }

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

        val cloudItems = when (val result = SavedContentClient.asyncFetchAll(context, userId)) {
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
            when (val result = SavedContentClient.asyncUnsave(context, key)) {
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
            when (val result = SavedContentClient.asyncSave(context, key)) {
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
}
