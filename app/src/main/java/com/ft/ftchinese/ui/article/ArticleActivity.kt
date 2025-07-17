package com.ft.ftchinese.ui.article

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
import androidx.core.app.TaskStackBuilder
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.ui.article.audio.AiAudioActivityScreen
import com.ft.ftchinese.ui.article.content.ArticleActivityScreen
import com.ft.ftchinese.ui.article.screenshot.ScreenshotActivityScreen
import com.ft.ftchinese.ui.components.Toolbar
import com.ft.ftchinese.ui.theme.OTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.MaterialTheme
import androidx.core.view.WindowCompat
import com.ft.ftchinese.ui.theme.OColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.SideEffect

/**
 * NOTE: after trial and error, as of Android Studio RC1, data binding class cannot be
 * properly generated for CoordinatorLayout.
 */
class ArticleActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge drawing
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Make status bar icons dark (good for light backgrounds like wheat)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true

        val teaser = intent
            .getParcelableExtra<Teaser>(EXTRA_ARTICLE_TEASER)
            ?: return

        setContent {
            val navColor = OColor.wheat.toArgb()
            SideEffect {
                window.navigationBarColor = navColor
            }
            ArticleApp(
                onExit = { finish() },
                initialId = NavStore.saveTeaser(teaser),
            )
        }
    }


    companion object {
        const val EXTRA_ARTICLE_TEASER = "com.ft.ftchinese.ArticleTeaser"

        @JvmStatic
        fun newIntent(context: Context?, teaser: Teaser?): Intent {
            return Intent(
                    context,
                    ArticleActivity::class.java
            ).apply {
                putExtra(EXTRA_ARTICLE_TEASER, teaser)
            }
        }

        /**
         * Load content with standard JSON API.
         */
        @JvmStatic
        fun start(context: Context?, teaser: Teaser) {
            val intent = Intent(context, ArticleActivity::class.java).apply {
                putExtra(EXTRA_ARTICLE_TEASER, teaser)
            }

            context?.startActivity(intent)
        }

        // When app is in background and user clicked notification message, open the activity with parent stack
        // so that back button works.
        @JvmStatic
        fun startWithParentStack(context: Context, channelItem: Teaser) {
            val intent = Intent(context, ArticleActivity::class.java).apply {
                putExtra(EXTRA_ARTICLE_TEASER, channelItem)
            }

            TaskStackBuilder
                .create(context)
                .addNextIntentWithParentStack(intent)
                .startActivities()
        }
    }
}

@Composable
private fun ArticleApp(
    onExit: () -> Unit,
    initialId: String,
) {

    val scaffoldState = rememberScaffoldState()
    val navController = rememberNavController()
    val backstackEntry = navController.currentBackStackEntryAsState()
    val currentScreen = ArticleAppScreen.fromRoute(
        backstackEntry.value?.destination?.route
    )

    OTheme {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.primary)
                .systemBarsPadding(),
            topBar = {
                if (currentScreen != ArticleAppScreen.Story) {
                    Toolbar(
                        heading = currentScreen.title,
                        onBack = {
                            val ok = navController.popBackStack()
                            if (!ok) {
                                onExit()
                            }
                        }
                    )
                }
            },
            scaffoldState = scaffoldState
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "${ArticleAppScreen.Story.name}/{id}",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(
                    route = "${ArticleAppScreen.Story.name}/{id}",
                    arguments = listOf(
                        navArgument("id") {
                            type = NavType.StringType
                            defaultValue = initialId
                        }
                    )
                ) { entry ->
                    val id = entry.arguments?.getString("id")
                    ArticleActivityScreen(
                        scaffoldState = scaffoldState,
                        id = id,
                        onScreenshot = {
                            navigate(
                                navController = navController,
                                screen = ArticleAppScreen.Screenshot,
                                id = it,
                            )
                        },
                        onAudio = {
                            navigate(
                                navController = navController,
                                screen = ArticleAppScreen.Audio,
                                id = it,
                            )
                        },
                        onArticle = {
                            navigate(
                                navController = navController,
                                screen = ArticleAppScreen.Story,
                                id = it
                            )
                        },
                        onChannel = {
                            navigate(
                                navController = navController,
                                screen = ArticleAppScreen.Channel,
                                id = it,
                            )
                        }
                    ) {
                        val ok = navController.popBackStack()
                        if (!ok) {
                            onExit()
                        }
                    }
                }

                composable(
                    route = "${ArticleAppScreen.Screenshot.name}/{id}",
                    arguments = listOf(
                        navArgument("id") {
                            type = NavType.StringType
                        },
                    )
                ) { entry ->
                    val id = entry.arguments?.getString("id")
                    ScreenshotActivityScreen(
                        id = id,
                    )
                }

                composable(
                    route = "${ArticleAppScreen.Audio.name}/{id}",
                    arguments = listOf(
                        navArgument("id") {
                            type = NavType.StringType
                        }
                    )
                ) { entry ->
                    val id = entry.arguments?.getString("id")
                    AiAudioActivityScreen(
                        id = id
                    )
                }
            }
        }
    }
}

private fun navigate(
    navController: NavController,
    screen: ArticleAppScreen,
    id: String
) {
    navController.navigate("${screen.name}/${id}")
}
