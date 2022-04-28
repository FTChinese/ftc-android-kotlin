package com.ft.ftchinese.store

import android.content.Context
import androidx.core.content.edit
import com.ft.ftchinese.model.AppRelease
import com.ft.ftchinese.model.fetch.formatISODateTime
import com.ft.ftchinese.model.fetch.marshaller
import com.ft.ftchinese.model.fetch.parseISODateTime
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.threeten.bp.ZonedDateTime

private const val FILE_NAME_DOWNLOAD = "com.ft.ftchinese.app_release"
private const val PREF_KEY_DOWNLOAD_ID = "download_id"
private const val PREF_KEY_LATEST = "latest_release"
private const val PREF_KEY_DOWNLOADING = "downloading_version"
private const val PREF_KEY_LAST_CHECKED = "last_checked_at"

class ReleaseStore(context: Context) {
    private val sharedPref = context
        .getSharedPreferences(
            FILE_NAME_DOWNLOAD,
            Context.MODE_PRIVATE,
        )

    fun saveLatest(release: AppRelease) {
        sharedPref.edit(commit = true) {
            putString(
                PREF_KEY_LATEST,
                marshaller.encodeToString(release),
            )
            putString(
                PREF_KEY_LAST_CHECKED,
                formatISODateTime(ZonedDateTime.now())
            )
        }
    }

    fun getLastCheckTime(): ZonedDateTime? {
        val checkedAt = sharedPref
            .getString(PREF_KEY_LAST_CHECKED, null)
            ?: return null

        return parseISODateTime(checkedAt)
    }

    fun loadLatest(): AppRelease? {
        val release = sharedPref
            .getString(PREF_KEY_LATEST, null)
            ?: return null

        return marshaller.decodeFromString(release)
    }

    fun saveDownloadId(id: Long, release: AppRelease) {
        sharedPref.edit(commit = true) {
            putLong(PREF_KEY_DOWNLOAD_ID, id)
            putString(PREF_KEY_DOWNLOADING, marshaller.encodeToString(release))
        }
    }

    fun loadDownloadId(): Pair<Long, AppRelease>? {
        val id = sharedPref.getLong(PREF_KEY_DOWNLOAD_ID, -1)
        val releaseStr = sharedPref
            .getString(PREF_KEY_DOWNLOADING, null) ?: return null

        return Pair(id, marshaller.decodeFromString(releaseStr))
    }

    fun clear() {
        sharedPref.edit(commit = true) {
            clear()
        }
    }
}