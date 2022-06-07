package com.ft.ftchinese.ui.settings.release

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.R
import com.ft.ftchinese.model.AppDownloaded
import com.ft.ftchinese.model.AppRelease
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.repository.ReleaseRepo
import com.ft.ftchinese.store.ReleaseStore
import com.ft.ftchinese.ui.base.ConnectionState
import com.ft.ftchinese.ui.base.connectivityState
import com.ft.ftchinese.ui.components.BaseState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "ReleaseState"

class ReleaseState(
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
    connState: State<ConnectionState>,
    context: Context
) : BaseState(scaffoldState, scope, context.resources, connState) {

    private val releaseStore = ReleaseStore(context)
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    var newRelease by mutableStateOf<AppRelease?>(null)
        private set

    var downloadStage by mutableStateOf<DownloadStage>(DownloadStage.NotStarted)
        private set

    private suspend fun loadCachedRelease(): AppDownloaded? {
        return try {
            withContext(Dispatchers.IO) {
                releaseStore.loadDownload()
            }
        } catch (e: Exception) {
            null
        }
    }

    fun loadRelease(useCache: Boolean) {

        progress.value = true

        scope.launch {
            if (useCache) {
                val cached = loadCachedRelease()
                if (cached != null) {
                    if (cached.release.isNew && cached.release.isValid) {
                        updatedReleaseStatus(cached)
                        progress.value = false
                        return@launch
                    }
                }
            }

            if (!ensureConnected()) {
                return@launch
            }

            when (val remote = ReleaseRepo.asyncGetLatest()) {
                is FetchResult.LocalizedError -> {
                    showSnackBar(remote.msgId)
                }
                is FetchResult.TextError -> {
                    showSnackBar(remote.text)
                }
                is FetchResult.Success -> {
                    val cached = withContext(Dispatchers.IO) {
                        releaseStore.saveLatest(remote.data)
                    }

                    Log.i(TAG, "Release downloaded: $cached")
                    updatedReleaseStatus(cached)
                }
            }

            progress.value = false
        }
    }

    // Check if apk for current release already exists.
    private fun updatedReleaseStatus(down: AppDownloaded) {
        Log.i(TAG, "$down")
        if (down.downloadId < 0) {
            newRelease = down.release
            return
        }

        val uri = getUriForApk(down.downloadId)
        Log.i(TAG, "Downloaded file uri: $uri")

        if (uri == null) {
            newRelease = down.release
            return
        }

        val exists = uri.path?.let {
            File(it).exists()
        } ?: false

        Log.i(TAG, "Downloaded file exists: $exists")
        if (exists) {
            downloadStage = DownloadStage.Completed(
                downloadId = down.downloadId
            )
        }

        newRelease = down.release
    }

    fun initDownload(release: AppRelease) {
        progress.value = true

        val downloadReq = buildDownloadRequest(
            release = release,
            resources = resources
        )
        if (downloadReq == null) {
            showSnackBar(R.string.download_not_found)
            progress.value = false
            return
        }

        val downloadId = downloadManager.enqueue(downloadReq)
        progress.value = false

        downloadStage = DownloadStage.Progress

        scope.launch {
            withContext(Dispatchers.IO) {
                releaseStore.saveDownload(downloadId, release)
            }
        }
    }

    fun removeApk(id: Long) {
        downloadManager.remove(id)
        downloadStage = DownloadStage.NotStarted

        getUriForApk(id)?.path?.let { localPath ->
            try {
                File(localPath).delete()
            } catch (e: Exception) {
                e.message?.let { showSnackBar(it) }
            }
        }
    }

    // Get the downloaded uri when we try to install it.
    // It seems the uri returned by DownloadManager.getUriForDownloadedFile
    // is not valid for installation.
    fun getUriForApk(id: Long): Uri? {

        val query = DownloadManager.Query().setFilterById(id)
        val c = downloadManager.query(query) ?: return null

        if (!c.moveToFirst()) {
            c.close()
            return null
        }

        return try {
            // Uri where downloaded file will be stored.
            val localUri = c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)) ?: return null

            Uri.parse(localUri)
        } catch (e: Exception) {
            null
        } finally {
            c.close()
        }
    }

    // Get download status when notification is clicked.
    private fun getDownloadStatus(id: Long): Int? {
        val query = DownloadManager.Query().setFilterById(id)
        val c = downloadManager.query(query) ?: return null

        if (!c.moveToFirst()) {
            c.close()
            return null
        }

        return try {
            val status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))

            status
        } catch (e: Exception) {
            null
        } finally {
            c.close()
        }
    }

    // Handle user clicking from the notification bar.
    val onNotificationClicked = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            val clickedId = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -2)

            if (clickedId == null) {
                showSnackBar(R.string.download_not_found)
                return
            }

            when (getDownloadStatus(clickedId)) {
                DownloadManager.STATUS_PENDING -> {
                    showSnackBar(R.string.download_pending)
                }
                DownloadManager.STATUS_PAUSED -> {
                    showSnackBar(R.string.download_paused)
                }
                DownloadManager.STATUS_RUNNING -> {
                    showSnackBar(R.string.download_running)
                }
                DownloadManager.STATUS_SUCCESSFUL -> {
                    downloadStage = DownloadStage.Completed(clickedId)
                }
                DownloadManager.STATUS_FAILED -> {
                    showSnackBar(R.string.download_failed)
                    downloadStage = DownloadStage.NotStarted
                }
                else -> {
                    showSnackBar("Unknown status")
                }
            }
        }
    }

    // Handle download complete
    val onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            val downloadId = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -2)

            downloadStage = if (downloadId != null) {
                DownloadStage.Completed(downloadId)
            } else {
                showSnackBar(R.string.download_not_found)
                DownloadStage.NotStarted
            }
        }
    }
}

private fun buildDownloadRequest(release: AppRelease, resources: Resources): DownloadManager.Request? {
    val parsedUri = Uri.parse(release.apkUrl)
    val fileName = parsedUri.lastPathSegment ?: return null

    Log.i(TAG, "Download to ${Environment.DIRECTORY_DOWNLOADS}")

    return try {
        DownloadManager.Request(parsedUri)
            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setTitle(resources.getString(R.string.download_title, release.versionName))
            .setMimeType("application/vnd.android.package-archive")
    } catch (e: Exception) {
        null
    }
}

@Composable
fun rememberReleaseState(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    connState: State<ConnectionState> = connectivityState(),
    scope: CoroutineScope = rememberCoroutineScope(),
    context: Context = LocalContext.current,
) = remember(scaffoldState, connState) {
    ReleaseState(
        scaffoldState = scaffoldState,
        scope = scope,
        connState = connState,
        context = context,
    )
}
