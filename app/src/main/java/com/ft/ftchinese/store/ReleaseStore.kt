package com.ft.ftchinese.store

import android.content.Context
import androidx.core.content.edit
import com.ft.ftchinese.model.AppDownloaded
import com.ft.ftchinese.model.AppRelease
import com.ft.ftchinese.model.fetch.marshaller
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

private const val FILE_NAME_DOWNLOAD = "com.ft.ftchinese.app_release"
private const val KEY_CHECK_ON_LAUNCH = "check_on_launch"
private const val PREF_KEY_DOWNLOAD_ID = "download_id"
private const val PREF_KEY_LATEST = "latest_release"
private const val PREF_KEY_DOWNLOADING = "downloading_version"

class ReleaseStore(context: Context) {
    private val sharedPref = context
        .getSharedPreferences(
            FILE_NAME_DOWNLOAD,
            Context.MODE_PRIVATE,
        )

    fun saveCheckOnLaunch(on: Boolean) {
        sharedPref.edit(commit = true) {
            putBoolean(KEY_CHECK_ON_LAUNCH, on)
        }
    }

    fun getCheckOnLaunch(): Boolean {
        return sharedPref.getBoolean(KEY_CHECK_ON_LAUNCH, true)
    }

    fun saveLatest(release: AppRelease): AppDownloaded {

        val cached = loadDownload()
        if (cached == null) {
            clearAndSave(release)
            return AppDownloaded(
                downloadId = -1,
                release = release
            )
        }

        return if (release.versionCode == cached.release.versionCode && cached.downloadId > 0) {
            saveDownload(cached.downloadId, release)
            AppDownloaded(
                downloadId = cached.downloadId,
                release = release
            )
        } else {
            clearAndSave(release)
            AppDownloaded(
                downloadId = -1,
                release = release,
            )
        }
    }

    private fun clearAndSave(release: AppRelease) {
        sharedPref.edit(commit = true) {
            clear()
            putString(
                PREF_KEY_LATEST,
                marshaller.encodeToString(release),
            )
        }
    }

    /**
     * Save download id together with the release data
     * so that we know which apk the id is referring to.
     */
    fun saveDownload(id: Long, release: AppRelease) {
        sharedPref.edit(commit = true) {
            putLong(PREF_KEY_DOWNLOAD_ID, id)
            putString(PREF_KEY_DOWNLOADING, marshaller.encodeToString(release))
        }
    }

    fun loadDownload(): AppDownloaded? {
        val id = sharedPref.getLong(PREF_KEY_DOWNLOAD_ID, -1)
        val releaseStr = sharedPref
            .getString(PREF_KEY_DOWNLOADING, null) ?: return null

        return AppDownloaded(
            release = marshaller.decodeFromString(releaseStr),
            downloadId = id,
        )
    }

    fun clear() {
        sharedPref.edit(commit = true) {
            clear()
        }
    }
}
