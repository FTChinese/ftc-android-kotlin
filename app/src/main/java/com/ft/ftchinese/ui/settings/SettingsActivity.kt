package com.ft.ftchinese.ui.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ft.ftchinese.ui.components.Toolbar
import com.ft.ftchinese.ui.settings.fcm.FcmActivityScreen
import com.ft.ftchinese.ui.settings.overview.PreferenceActivityScreen
import com.ft.ftchinese.ui.theme.OTheme

// Reference: https://developer.android.com/guide/topics/ui/settings
class SettingsActivity : ComponentActivity() {

    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SettingsApp {
                finish()
            }
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, SettingsActivity::class.java))
        }
    }
}

@Composable
fun SettingsApp(
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
                Toolbar(
                    heading = stringResource(id = currentScreen.titleId),
                    onBack = {
                        val ok = navController.popBackStack()
                        if (!ok) {
                            onExit()
                        }
                    }
                )
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
                        }
                    )
                }

                composable(
                    route = SettingScreen.Notification.name
                ) {
                    FcmActivityScreen()
                }

                composable(
                    route = SettingScreen.CheckVersion.name
                ) {

                }
            }
        }
    }
}

private fun navigateToScreen(
    navController: NavController,
    screen: SettingScreen
) {
    navController.navigate(screen.name)
}
