package com.ft.ftchinese.ui.settings.release

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.toast
import com.ft.ftchinese.ui.components.Toolbar
import com.ft.ftchinese.ui.dialog.AlertDialogFragment
import com.ft.ftchinese.ui.dialog.DialogArgs
import com.ft.ftchinese.ui.settings.SettingScreen
import com.ft.ftchinese.ui.theme.OTheme
import java.io.File

class ReleaseActivity : ScopedAppActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ReleaseApp(
                onInstall = this::initInstall,
                onExit =  {
                    finish()
                }
            )
        }
    }

    private fun initInstall(apkUri: Uri) {
        // Path removes the scheme part of file://
        // Example: /storage/emulated/0/Download/ftchinese-v6.3.4-ftc-release.apk
        val filePath = apkUri.path

        if (filePath == null) {
            toast("Downloaded file uri cannot be parsed")
            return
        }

        val downloadedFile = File(filePath)

        try {
            install(downloadedFile)
        } catch (e: Exception) {
            AlertDialogFragment.newInstance(
                    DialogArgs(
                        message = e.message ?: "",
                        positiveButton = R.string.btn_retry,
                        negativeButton = R.string.btn_cancel,
                        title = R.string.installation_failed
                    )
                )
                .onPositiveButtonClicked { dialog, _ ->
                    dialog.dismiss()
                    install(downloadedFile)
                }
                .onNegativeButtonClicked { dialog, _ ->
                    dialog.dismiss()
                }
                .show(supportFragmentManager, "InstallFailed")
        }
    }

    private fun install(file: File) {

        val contentUri = buildContentUri(this, file)

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
            putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, applicationInfo.packageName)
        }

        startActivity(intent)
    }

    companion object {

        @JvmStatic
        fun deepLinkIntent(context: Context) = Intent(
            Intent.ACTION_VIEW,
            SettingScreen.newReleaseDeepLink.toUri(),
            context,
            ReleaseActivity::class.java
        )

        @JvmStatic
        fun start(context: Context?) {
            val intent = Intent(context, ReleaseActivity::class.java)
            context?.startActivity(intent)
        }
    }
}

@Composable
private fun ReleaseApp(
    onInstall: (Uri) -> Unit,
    onExit: () -> Unit
) {
    val navController = rememberNavController()

    OTheme {
        val scaffoldState = rememberScaffoldState()

        Scaffold(
            topBar = {
                Toolbar(
                    heading = stringResource(id = R.string.pref_check_new_version),
                    onBack = onExit
                )
            },
            scaffoldState = scaffoldState
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = SettingScreen.releaseRoutePattern,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(
                    route = SettingScreen.releaseRoutePattern,
                    arguments = listOf(
                        navArgument("cached") {
                            type = NavType.BoolType
                            defaultValue = false
                        }
                    ),
                    deepLinks = listOf(
                        navDeepLink {
                            uriPattern = SettingScreen.releaseDeepLinkPattern
                        }
                    )
                ) { entry ->
                    val cached = entry.arguments?.getBoolean("cached") ?: false
                    ReleaseActivityScreen(
                        scaffoldState = scaffoldState,
                        cached = cached,
                        onInstall = onInstall,
                    )
                }
            }

        }
    }
}

// Build file uri of downloaded file when we try to install it.
// Turn file to a content uri so that it could be shared with installer:
// content://com.ft.ftchinese.fileprovider/my_download/ftchinese-v6.3.4-ftc-release.apk
private fun buildContentUri(context: Context, file: File): Uri {
    return FileProvider
        .getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            file
        )
}
