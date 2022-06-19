package com.ft.ftchinese.ui.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.ft.ftchinese.ui.components.Toolbar
import com.ft.ftchinese.ui.settings.about.AboutActivityScreen
import com.ft.ftchinese.ui.settings.about.LegalDocActivityScreen
import com.ft.ftchinese.ui.settings.fcm.FcmActivityScreen
import com.ft.ftchinese.ui.settings.overview.PreferenceActivityScreen
import com.ft.ftchinese.ui.settings.overview.SettingScreen
import com.ft.ftchinese.ui.settings.release.ReleaseActivityScreen
import com.ft.ftchinese.ui.theme.OTheme
import com.ft.ftchinese.ui.util.IntentsUtil

// Reference: https://developer.android.com/guide/topics/ui/settings
class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SettingsApp(
                onLoggedOut = {
                    // Immediately close current screen to force UI update; otherwise
                    // the UI does not update if user exit by pressing the back key
                    // rather than clicking the toolbar back button.
                    // Back key press won't trigger setResult().
                    setResult(Activity.RESULT_OK, IntentsUtil.loggedOut)
                    finish()
                },
                onExit = {
                    finish()
                }
            )
        }
    }

    companion object {

        // Handle release notification.
        @JvmStatic
        fun deepLinkIntent(context: Context) = Intent(
            Intent.ACTION_VIEW,
            SettingScreen.newReleaseDeepLink.toUri(),
            context,
            SettingsActivity::class.java
        )

        // If user logged out, notify the calling activity.
        @JvmStatic
        fun launch(
            launcher: ManagedActivityResultLauncher<Intent, ActivityResult>,
            context: Context,
        ) {
            launcher.launch(
                Intent(context, SettingsActivity::class.java)
            )
        }

        @JvmStatic
        fun start(context: Context) {
            context.startActivity(Intent(context, SettingsActivity::class.java))
        }
    }
}

@Composable
fun SettingsApp(
    onLoggedOut: () -> Unit,
    onExit: () -> Unit,
) {
    val scaffoldState = rememberScaffoldState()

    val navController = rememberNavController()
    val backstackEntry = navController.currentBackStackEntryAsState()
    val currentScreen = SettingScreen.fromRoute(
        backstackEntry.value?.destination?.route
    )

    OTheme {
        Scaffold(
            topBar = {
                if (currentScreen.showToolBar) {
                    Toolbar(
                        heading = stringResource(id = currentScreen.titleId),
                        onBack = {
                            val ok = navController.popBackStack()
                            if (!ok) {
                                onExit()
                            }
                        },
                    )
                }

            },
            scaffoldState = scaffoldState,
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = SettingScreen.Overview.name,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(
                    route = SettingScreen.Overview.name
                ) {
                    PreferenceActivityScreen(
                        scaffoldState = scaffoldState,
                        onNavigateTo =  { screen ->
                            navigateToScreen(
                                navController,
                                screen,
                            )
                        },
                        onLoggedOut = onLoggedOut
                    )
                }

                composable(
                    route = SettingScreen.Notification.name
                ) {
                    FcmActivityScreen()
                }

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
                        cached = cached
                    )
                }

                composable(
                    route = SettingScreen.AboutUs.name
                ) {
                    AboutActivityScreen(
                        onNavigate = {
                            navigateToLegal(
                                navController = navController,
                                index = it,
                            )
                        }
                    )
                }

                composable(
                    route = "${SettingScreen.Legal.name}/{index}",
                    arguments = listOf(
                        navArgument("index") {
                            type = NavType.IntType
                        }
                    )
                ) { entry ->
                    val index = entry.arguments?.getInt("index") ?: 0
                    LegalDocActivityScreen(
                        index = index,
                        onClose = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}

private fun navigateToScreen(
    navController: NavController,
    screen: SettingScreen
) {
    when (screen) {
        SettingScreen.CheckVersion -> {
            navController.navigate(SettingScreen.newReleaseRoute)
        }
        else -> {
            navController.navigate(screen.name)
        }
    }
}

private fun navigateToLegal(
    navController: NavController,
    index: Int,
) {
    navController.navigate("${SettingScreen.Legal.name}/${index}")
}
