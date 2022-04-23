package com.ft.ftchinese.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ft.ftchinese.R
import com.ft.ftchinese.model.legal.WebpageMeta
import com.ft.ftchinese.ui.about.AboutActivityScreen
import com.ft.ftchinese.ui.about.AboutDetailsActivityScreen
import com.ft.ftchinese.ui.components.Toolbar
import com.ft.ftchinese.ui.theme.OTheme

class AboutListActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AboutApp {
                finish()
            }
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, AboutListActivity::class.java))
        }
    }
}

enum class AboutScreen {
    Overview,
    Details;

    companion object {
        fun fromRoute(route: String?): AboutScreen =
            when (route?.substringBefore("/")) {
                Overview.name -> Overview
                Details.name -> Details
                null -> Overview
                else -> throw IllegalArgumentException("Route $route is not recognized")
            }
    }
}

@Composable
fun AboutApp(
    onExit: () -> Unit
) {
    val context = LocalContext.current

    val homePageMeta = WebpageMeta(
        title = context.getString(R.string.title_about_us),
        url = "",
        showMenu = false
    )

    val scaffoldState = rememberScaffoldState()
    val (pageMeta, setPageMeta) = remember {
        mutableStateOf(homePageMeta)
    }

    OTheme {
        val navController = rememberNavController()
        val backstackEntry = navController.currentBackStackEntryAsState()
        val currentScreen = AboutScreen.fromRoute(
            backstackEntry.value?.destination?.route
        )
        if (currentScreen == AboutScreen.Overview) {
            setPageMeta(homePageMeta)
        }

        Scaffold(
            topBar = {
                Toolbar(
                    heading = pageMeta.title,
                    onBack = {
                        val ok = navController.popBackStack()
                        if (!ok) {
                            onExit()
                        }
                    },
                    actions = {
                        if (pageMeta.showMenu) {
                            ToolbarMenu {
                                CustomTabsIntent
                                    .Builder()
                                    .build()
                                    .launchUrl(
                                        context,
                                        Uri.parse(pageMeta.url)
                                    )
                            }
                        }
                    }
                )
            },
            scaffoldState = scaffoldState
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = AboutScreen.Overview.name,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(
                    route = AboutScreen.Overview.name
                ) {
                    AboutActivityScreen(
                        onNavigate = {
                            setPageMeta(it)
                            navigateToDetails(
                                navController = navController,
                                url = it.url
                            )
                        }
                    )
                }

                composable(
                    route = "${AboutScreen.Details.name}/?url={url}",
                    arguments = listOf(
                        navArgument("url") {
                            type = NavType.StringType
                        }
                    )
                ) { entry ->
                    val url = entry.arguments?.getString("url")!!
                    AboutDetailsActivityScreen(
                        url = url
                    )
                }
            }
        }
    }
}

private fun navigateToDetails(
    navController: NavController,
    url: String,
) {
    navController.navigate("${AboutScreen.Details.name}/?url=${url}")
}

@Composable
fun ToolbarMenu(
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_baseline_open_in_browser_24),
            contentDescription = "Open in browser"
        )
    }
}
