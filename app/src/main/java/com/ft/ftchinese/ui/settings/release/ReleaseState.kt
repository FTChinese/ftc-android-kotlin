package com.ft.ftchinese.ui.settings.release

import android.app.DownloadManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.res.Resources
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.model.AppDownloaded
import com.ft.ftchinese.model.AppRelease
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.repository.ReleaseRepo
import com.ft.ftchinese.store.ReleaseStore
import com.ft.ftchinese.ui.components.BaseState
import com.ft.ftchinese.ui.util.ConnectionState
import com.ft.ftchinese.ui.util.connectivityState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "ReleaseState"
private const val PI_INSTALL = 3439

class ReleaseState(
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
    connState: State<ConnectionState>,
    val context: Context
) : BaseState(scaffoldState, scope, context.resources, connState) {

    private val releaseStore = ReleaseStore(context)
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    // Reference: https://gitlab.com/commonsguy/cw-android-q/blob/vFINAL/AppInstaller/src/main/java/com/commonsware/q/appinstaller/MainMotor.kt
    private val installer = context.packageManager.packageInstaller
    private val resolver = context.contentResolver

    var checkOnLaunch by mutableStateOf(releaseStore.getCheckOnLaunch())
        private set

    var newRelease by mutableStateOf<AppRelease?>(null)
        private set

    var downloadStage by mutableStateOf<DownloadStage>(DownloadStage.NotStarted)
        private set

    fun switchCheckOnLaunch(on: Boolean) {
        checkOnLaunch = on
        releaseStore.saveCheckOnLaunch(on)
    }

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
    private fun getUriForApk(id: Long): Uri? {

        val query = DownloadManager.Query().setFilterById(id)
        val c = downloadManager.query(query) ?: return null

        if (!c.moveToFirst()) {
            c.close()
            return null
        }

        return try {
            // Uri where downloaded file will be stored.
            val localUri = c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)) ?: return null
            // Something like file:///storage/emulated/0/Download/ftchinese-v6.8.0-ftc-release.apk
            Log.i(TAG, "Downloaded APK uri: $localUri")
            Uri.parse(localUri)
        } catch (e: Exception) {
            null
        } finally {
            c.close()
        }
    }

    // Build file uri of downloaded file when we try to install it.
    // Turn file to a content uri so that it could be shared with installer:
    // content://com.ft.ftchinese.fileprovider/my_download/ftchinese-v6.3.4-ftc-release.apk
    private fun buildInstallerUri(
        apkUri: Uri
    ): Uri? {
        // Path removes the scheme part of file://
        // Example: /storage/emulated/0/Download/ftchinese-v6.3.4-ftc-release.apk
        val filePath = apkUri.path ?: return null

        val file = File(filePath)

        return FileProvider
            .getUriForFile(
                context,
                "${BuildConfig.APPLICATION_ID}.fileprovider",
                file
            )
    }

    fun getInstallerUri(downloadId: Long): Uri? {
        val apkUri = getUriForApk(downloadId)
        Log.i(TAG, "APK uri: $apkUri")
        if (apkUri == null) {
            showSnackBar(R.string.download_failed)
            removeApk(downloadId)
            return null
        }

        val installerUri = buildInstallerUri(apkUri)
        Log.i(TAG, "Installer uri: $installerUri")
        return if (installerUri == null) {
            showSnackBar(R.string.download_failed)
            null
        } else {
            installerUri
        }
    }

    fun install(apkUri: Uri) {
        scope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                Log.i(TAG, "Open apk stream")
                resolver.openInputStream(apkUri)?.use { apkStream ->
                    val length = DocumentFile.fromSingleUri(context, apkUri)?.length() ?: -1
                    Log.i(TAG, "APK length $length")
                    val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
                    val sessionId = installer.createSession(params)
                    val session = installer.openSession(sessionId)
                    Log.i(TAG, "Install session created")

                    session.openWrite("com.ft.ftchinese", 0, length).use { sessionStream ->
                        apkStream.copyTo(sessionStream)
                        session.fsync(sessionStream)
                    }

                    val intent = Intent(context, InstallReceiver::class.java)
                    val pi = PendingIntent.getBroadcast(
                        context,
                        PI_INSTALL,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )

                    session.commit(pi.intentSender)
                    Log.i(TAG, "Commit session")
                    session.close()
                    Log.i(TAG, "Close session")
                }
            }
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
