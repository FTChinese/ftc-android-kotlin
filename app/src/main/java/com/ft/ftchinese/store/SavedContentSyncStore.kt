package com.ft.ftchinese.store

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.ft.ftchinese.database.StarredArticle
import com.ft.ftchinese.repository.SavedContentClient

private const val PREF_NAME = "saved_content_sync"
private const val KEY_LOCAL_OWNER = "local_owner"
private const val KEY_LAST_SYNCED_USER = "last_synced_user"

class SavedContentSyncStore private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun localOwner(): String? {
        return prefs.getString(KEY_LOCAL_OWNER, null)?.takeIf { it.isNotBlank() }
    }

    fun setLocalOwner(userId: String) {
        prefs.edit {
            putString(KEY_LOCAL_OWNER, userId)
        }
    }

    fun lastSyncedUser(): String? {
        return prefs.getString(KEY_LAST_SYNCED_USER, null)?.takeIf { it.isNotBlank() }
    }

    fun hasSyncedSnapshot(userId: String): Boolean {
        return prefs.contains(keyFor(userId, "synced"))
    }

    fun syncedKeys(userId: String): Set<String> {
        return readSet(userId, "synced")
    }

    fun pendingSaveKeys(userId: String): Set<String> {
        return readSet(userId, "pending_save")
    }

    fun pendingUnsaveKeys(userId: String): Set<String> {
        return readSet(userId, "pending_unsave")
    }

    fun markLocalSave(userId: String, article: StarredArticle) {
        if (userId.isBlank() || !SavedContentClient.isSyncableLocalType(article.type)) {
            return
        }

        val key = SavedContentClient.savedContentKey(article)
        val pendingSaves = pendingSaveKeys(userId).toMutableSet()
        val pendingUnsaves = pendingUnsaveKeys(userId).toMutableSet()
        pendingSaves.add(key)
        pendingUnsaves.remove(key)

        prefs.edit {
            putStringSet(keyFor(userId, "pending_save"), pendingSaves)
            putStringSet(keyFor(userId, "pending_unsave"), pendingUnsaves)
        }
    }

    fun markLocalUnsave(userId: String, id: String, type: String) {
        if (userId.isBlank() || !SavedContentClient.isSyncableLocalType(type)) {
            return
        }

        val key = SavedContentClient.savedContentKey(id, type)
        val pendingSaves = pendingSaveKeys(userId).toMutableSet()
        val pendingUnsaves = pendingUnsaveKeys(userId).toMutableSet()
        pendingSaves.remove(key)
        pendingUnsaves.add(key)

        prefs.edit {
            putStringSet(keyFor(userId, "pending_save"), pendingSaves)
            putStringSet(keyFor(userId, "pending_unsave"), pendingUnsaves)
        }
    }

    fun markSynced(userId: String, keys: Set<String>) {
        prefs.edit {
            putString(KEY_LOCAL_OWNER, userId)
            putString(KEY_LAST_SYNCED_USER, userId)
            putLong(keyFor(userId, "last_sync"), System.currentTimeMillis())
            putStringSet(keyFor(userId, "synced"), keys)
            putStringSet(keyFor(userId, "pending_save"), emptySet())
            putStringSet(keyFor(userId, "pending_unsave"), emptySet())
        }
    }

    private fun readSet(userId: String, suffix: String): Set<String> {
        return prefs.getStringSet(keyFor(userId, suffix), emptySet())
            ?.toSet()
            ?: emptySet()
    }

    private fun keyFor(userId: String, suffix: String): String {
        return "$userId:$suffix"
    }

    companion object {
        @Volatile private var instance: SavedContentSyncStore? = null

        fun getInstance(ctx: Context): SavedContentSyncStore =
            instance ?: synchronized(this) {
                instance ?: SavedContentSyncStore(ctx.applicationContext).also { instance = it }
            }
    }
}
