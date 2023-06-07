package com.ft.ftchinese.ui.settings.release

import android.app.DownloadManager
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.util.toast

@Composable
fun ReleaseActivityScreen(
    scaffoldState: ScaffoldState,
    cached: Boolean,
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

//    val launcher = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.RequestPermission()
//    ) { isGranted ->
//        if (isGranted) {
//            Log.e(TAG, "Permission granted")
//        } else {
//            context.toast("Permission to access external storage denied!")
//        }
//    }

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
        loading = releaseState.progress.value,
        modifier = Modifier.fillMaxSize()
    ) {
        ReleaseScreen(
            menu = {
                ReleaseMenu(
                    checkOnLaunch = releaseState.checkOnLaunch,
                    onToggleCheck = releaseState::switchCheckOnLaunch,
                    onDeleteDownload = {
                        releaseState.removeAnyApk()
                    },
                    onOpenDownloadsFolder = {
                        context.startActivity(
                            Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
                        )
                    },
                    onRefreshRelease = {
                        releaseState.loadRelease(useCache = false)
                    },
                    progress = releaseState.progress.value
                )
            }
        ) {
            releaseState.newRelease?.let {
                DownloadMenu(
                    loading = releaseState.progress.value,
                    downloadStage = releaseState.downloadStage,
                    release = it,
                    onClick = { release ->
                        when (releaseState.downloadStage) {
                            is DownloadStage.NotStarted -> {
                                releaseState.initDownload(release)
                                setShowDialog(true)
                            }
                            is DownloadStage.Progress -> {
                                context.toast("Please wait for download to finish")
                            }
                            is DownloadStage.Completed -> {
                                context.startActivity(
                                    Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
                                )
                            }
                        }
                    },
                )
            }
        }
    }
}

