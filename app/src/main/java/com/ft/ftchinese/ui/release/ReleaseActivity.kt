package com.ft.ftchinese.ui.release

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.model.AppRelease
import com.ft.ftchinese.ui.base.toast
import com.ft.ftchinese.ui.components.ShowToast
import com.ft.ftchinese.ui.components.Toolbar
import com.ft.ftchinese.ui.theme.OTheme
import org.jetbrains.anko.alert
import org.jetbrains.anko.appcompat.v7.Appcompat
import java.io.File

class ReleaseActivity : ComponentActivity() {

    private lateinit var releaseViewModel: ReleaseViewModel
    private lateinit var downloadManager: DownloadManager

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
    private val onNotificationClicked = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            val savedId = releaseViewModel.loadDownloadId()

            val clickedId = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -2)

            if (clickedId == null || savedId != clickedId) {
                toast(R.string.download_not_found)
                return
            }

            when (getDownloadStatus(clickedId)) {
                DownloadManager.STATUS_PENDING -> {
                    toast(R.string.download_pending)
                }
                DownloadManager.STATUS_PAUSED -> {
                    toast(R.string.download_paused)
                }
                DownloadManager.STATUS_RUNNING -> {
                    toast(R.string.download_running)
                }
                DownloadManager.STATUS_SUCCESSFUL -> {
                    releaseViewModel.downloadCompleted()
                    initInstall(clickedId)
                }
                DownloadManager.STATUS_FAILED -> {
                    toast(R.string.download_failed)
                }
                else -> {
                    toast("Unknown status")
                }
            }
        }
    }

    // Handle download complete
    private val onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val savedId = releaseViewModel.loadDownloadId() ?: return
            if (savedId < 0) {
                toast("Cannot locate download id")
                return
            }

            val notiId = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -2)

            if (savedId == notiId) {
                releaseViewModel.downloadCompleted()

                initInstall(notiId)
            } else {
                toast(R.string.download_not_found)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        releaseViewModel = ViewModelProvider(this)[ReleaseViewModel::class.java]
        downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        setContent {
            OTheme {
                val scaffoldState = rememberScaffoldState()

                Scaffold(
                    topBar = {
                        Toolbar(
                            heading = stringResource(id = R.string.pref_check_new_version),
                            onBack = { finish() }
                        )
                    },
                    scaffoldState = scaffoldState
                ) { innerPadding ->
                    ReleaseActivityScreen(
                        releaseViewModel = releaseViewModel,
                        modifier = Modifier.padding(innerPadding),
                        downloadManager = downloadManager,
                        onInstall = {
                            releaseViewModel.loadDownloadId()?.let {
                                initInstall(it)
                            }
                        }
                    )
                }
            }
        }

        // Broadcast intent action sent by the download manager when the user clicks on a running download,
        // either from a system notification or from the downloads UI.
        registerReceiver(onNotificationClicked, IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED))
        // Broadcast intent action sent by the download manager when a download completes.
        registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    // Get the downloaded uri when we try to install it.
    private fun retrieveDownloadUri(id: Long): Uri? {

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

    private fun initInstall(id: Long) {

        val localUri = retrieveDownloadUri(id)
        if (localUri == null) {
            toast("Downloaded file not found")
            return
        }

        val filePath = localUri.path

        if (filePath == null) {
            toast("Download file uri cannot be parsed")
            return
        }

        val downloadedFile = File(filePath)

        try {
            install(downloadedFile)
        } catch (e: Exception) {
            alert(Appcompat, "${e.message}", "Installation Failed") {
                positiveButton("Re-try") {
                    it.dismiss()
                    install(downloadedFile)
                }
                positiveButton("Cancel") {

                    it.dismiss()
                }
            }.show()
        }
    }

    private fun install(file: File) {
        val contentUri = buildContentUri(this, file)

        // Do not use ACTION_VIEW you found on most
        // stack overflow answers. It's too old.
        // Nor should you use ACTION_INSTALL_PACKAGE.
        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            setDataAndType(contentUri, "application/vnd.android.package-archive")
            // The permission must be added, otherwise you
            // will get error parsing package.
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, applicationInfo.packageName)
        }

        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(onNotificationClicked)
        unregisterReceiver(onDownloadComplete)
    }

    companion object {
        @JvmStatic
        fun newIntent(context: Context?): Intent {
            return Intent(
                context,
                ReleaseActivity::class.java
            )
        }

        @JvmStatic
        fun start(context: Context?) {
            val intent = Intent(context, ReleaseActivity::class.java)
            context?.startActivity(intent)
        }
    }
}

@Composable
private fun ReleaseActivityScreen(
    releaseViewModel: ReleaseViewModel,
    modifier: Modifier = Modifier,
    downloadManager: DownloadManager,
    onInstall: () -> Unit,
) {
    val context = LocalContext.current

    val progress by releaseViewModel.progressLiveData.observeAsState(true)
    val newRelease by releaseViewModel.newReleaseLiveData.observeAsState()
    val toast by releaseViewModel.toastLiveData.observeAsState()

    val (showDialog, setShowDialog) = remember {
        mutableStateOf(false)
    }

    val downloadStatus by releaseViewModel.downloadStatus.observeAsState(DownloadStatus.NotStarted)

    val currentVersion = remember {
        BuildConfig.VERSION_CODE
    }

    LaunchedEffect(key1 = Unit) {
        releaseViewModel.loadRelease()
    }

    if (showDialog) {
        AlertDownloadStart {
            setShowDialog(false)
        }
    }

    ShowToast(toast = toast) {
        releaseViewModel.resetToast()
    }

    ReleaseScreen(
        loading = progress,
        downloadStatus = downloadStatus,
        newRelease = newRelease,
        currentVersion = currentVersion,
        modifier = modifier,
        onClick = { status, release ->
            when (status) {
                DownloadStatus.NotStarted -> {
                    val id = launchDownload(
                        context = context,
                        downloadManager = downloadManager,
                        release = release,
                    ) ?: return@ReleaseScreen

                    releaseViewModel.downloadStart(id, release)
                    setShowDialog(true)
                }
                DownloadStatus.Progress -> {

                }
                DownloadStatus.Completed -> onInstall()
            }
        }
    )
}

private fun launchDownload(
    context: Context,
    downloadManager: DownloadManager,
    release: AppRelease
): Long? {
    val req = buildDownloadRequest(
        context = context,
        release = release
    )

    if (req == null) {
        context.toast(R.string.download_not_found)
        return null
    }

    return downloadManager.enqueue(req)
}

// Build download request after user clicked download button
private fun buildDownloadRequest(context: Context, release: AppRelease): DownloadManager.Request? {
    val parsedUri = Uri.parse(release.apkUrl)
    val fileName = parsedUri.lastPathSegment ?: return null

    return try {
        DownloadManager.Request(parsedUri)
            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setTitle(context.getString(R.string.download_title, release.versionName))
            .setMimeType("application/vnd.android.package-archive")
    } catch (e: Exception) {
        null
    }
}

// Build file uri of downloaded file when we try to install it.
private fun buildContentUri(context: Context, file: File): Uri {
    return FileProvider
        .getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            file
        )
}
