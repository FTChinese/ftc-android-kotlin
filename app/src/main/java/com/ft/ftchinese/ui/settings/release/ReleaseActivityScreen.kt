package com.ft.ftchinese.ui.settings.release

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.components.SimpleDialog
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
    
    val (installFailed, setInstallFailed) = remember {
        mutableStateOf<Uri?>(null)
    }

    LaunchedEffect(key1 = Unit) {
        releaseState.loadRelease(cached)
    }

    if (showDialog) {
        AlertDownloadStart {
            setShowDialog(false)
        }
    }
    
    installFailed?.let {
        SimpleDialog(
            title = stringResource(id = R.string.installation_failed), 
            body = "", 
            onDismiss = { setInstallFailed(null) }, 
            onConfirm = { 
                install(
                    context = context,
                    contentUri = it
                )
                setInstallFailed(null)
            },
            confirmText = stringResource(id = R.string.btn_retry)
        )
    }

    ProgressLayout(
        loading = releaseState.progress.value,
        modifier = Modifier.fillMaxSize()
    ) {
        ReleaseScreen(
            loading = releaseState.progress.value,
            checked = releaseState.checkOnLaunch,
            onCheckedChange = releaseState::switchCheckOnLaunch,
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

                        val installerUri = releaseState.getInstallerUri(stage.downloadId) ?: return@ReleaseScreen

//                        when (PackageManager.PERMISSION_GRANTED) {
//                            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) -> {
//                                Log.i(TAG, "Start install apk")
//                                releaseState.install(installerUri)
//                            }
//                            else -> {
//                                launcher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
//                            }
//                        }

                        try {
                            install(
                                context = context,
                                contentUri = installerUri
                            )
                        } catch (e: Exception) {
                            setInstallFailed(installerUri)
                        }
                    }
                }
            },
            onDelete =  {
                releaseState.removeApk(it)
            },
            onViewDownloads = {
                context.startActivity(
                    Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
                )
            }
        )
    }
}

private fun install(
    context: Context,
    contentUri: Uri
) {

    // Do not use ACTION_VIEW you found on most
    // stack overflow answers. It's too old.
    // Nor should you use ACTION_INSTALL_PACKAGE.
    // https://android.googlesource.com/platform/development/+/master/samples/ApiDemos/src/com/example/android/apis/content/InstallApk.java
    // New API: https://android.googlesource.com/platform/development/+/master/samples/ApiDemos/src/com/example/android/apis/content/InstallApkSessionApi.java
    val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
        setDataAndType(contentUri, "application/vnd.android.package-archive")
        // The permission must be added, otherwise you
        // will get error parsing package.
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
        putExtra(
            Intent.EXTRA_INSTALLER_PACKAGE_NAME, 
            context.applicationInfo.packageName
        )
    }

    context.startActivity(intent)
}
