package com.ft.ftchinese.ui.settings.release

import android.app.DownloadManager
import android.content.IntentFilter
import android.net.Uri
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.base.toast
import com.ft.ftchinese.ui.components.ProgressLayout

@Composable
fun ReleaseActivityScreen(
    scaffoldState: ScaffoldState,
    cached: Boolean,
    onInstall: (Uri) -> Unit,
) {
    val context = LocalContext.current

    val releaseState = rememberReleaseState(
        scaffoldState = scaffoldState
    )

    DisposableEffect(key1 = Unit) {

        // Broadcast intent action sent by the download manager when the user clicks on a running download,
        // either from a system notification or from the downloads UI.
        context.registerReceiver(
            releaseState.onNotificationClicked,
            IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED)
        )

        // Broadcast intent action sent by the download manager when a download completes.
        context.registerReceiver(
            releaseState.onDownloadComplete,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )

        onDispose {
            context.unregisterReceiver(releaseState.onNotificationClicked)
            context.unregisterReceiver(releaseState.onDownloadComplete)
        }
    }

    val (showDialog, setShowDialog) = remember {
        mutableStateOf(false)
    }

    LaunchedEffect(key1 = Unit) {
        releaseState.loadRelease(cached)
    }

    if (showDialog) {
        AlertDownloadStart {
            setShowDialog(false)
        }
    }

    ProgressLayout(
        loading = releaseState.progress.value
    ) {
        ReleaseScreen(
            loading = releaseState.progress.value,
            downloadStage = releaseState.downloadStage,
            newRelease = releaseState.newRelease,
            onClick = { release ->
                when (val stage = releaseState.downloadStage) {
                    is DownloadStage.NotStarted -> {
                        releaseState.initDownload(release)
                        setShowDialog(true)
                    }
                    is DownloadStage.Progress -> {
                        context.toast("Please wait for download to finish")
                    }
                    is DownloadStage.Completed -> {
                        val apkUri = releaseState.getUriForApk(stage.downloadId)
                        if (apkUri == null) {
                            context.toast(R.string.download_failed)
                            releaseState.removeApk(stage.downloadId)
                        } else {
                            onInstall(apkUri)
                        }
                    }
                }
            }
        ) {
            releaseState.removeApk(it)
        }
    }
}

