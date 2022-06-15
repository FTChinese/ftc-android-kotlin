package com.ft.ftchinese.ui.article

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
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
import com.ft.ftchinese.ui.article.screenshot.ScreenshotParams
import com.ft.ftchinese.ui.components.Toolbar
import com.ft.ftchinese.ui.theme.OTheme

/**
 * NOTE: after trial and error, as of Android Studio RC1, data binding class cannot be
 * properly generated for CoordinatorLayout.
 */
class ArticleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val teaser = intent
            .getParcelableExtra<Teaser>(EXTRA_ARTICLE_TEASER)
            ?: return

        setContent {
            ArticleApp(
                onExit = { finish() },
                teaser = teaser
            )
        }
    }

    companion object {
        const val EXTRA_ARTICLE_TEASER = "extra_article_teaser"

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
        fun start(context: Context?, channelItem: Teaser) {
            val intent = Intent(context, ArticleActivity::class.java).apply {
                putExtra(EXTRA_ARTICLE_TEASER, channelItem)
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
    teaser: Teaser,
) {

    val scaffoldState = rememberScaffoldState()
    val navController = rememberNavController()
    val backstackEntry = navController.currentBackStackEntryAsState()
    val currentScreen = ArticleAppScreen.fromRoute(
        backstackEntry.value?.destination?.route
    )

    OTheme {
        Scaffold(
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
                startDestination = ArticleAppScreen.Story.name,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(
                    route = ArticleAppScreen.Story.name
                ) {
                    ArticleActivityScreen(
                        scaffoldState = scaffoldState,
                        teaser = teaser,
                        onScreenshot = {
                            navigateToScreenshot(
                                navController,
                                it,
                            )
                        },
                        onAudio = {
                            navigateToAiAudio(
                                navController,
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
                    route = "${ArticleAppScreen.Screenshot.name}/{type}/{id}?imageUrl={imageUrl}",
                    arguments = listOf(
                        navArgument("id") {
                            type = NavType.StringType
                        },
                        navArgument("type") {
                            type = NavType.StringType
                        },
                        navArgument("imageUrl") {
                            type = NavType.StringType
                        },
                    )
                ) { entry ->
                    val id = entry.arguments?.getString("id")
                    val type = entry.arguments?.getString("type")
                    val imageUrl = entry.arguments?.getString("imageUrl")
                    ScreenshotActivityScreen(
                        type = type,
                        id = id,
                        imageUrl = imageUrl,
                    )
                }

                composable(
                    route = ArticleAppScreen.Audio.name,
                ) {
                    AiAudioActivityScreen()
                }
            }
        }
    }
}

private fun navigateToScreenshot(
    navController: NavController,
    params: ScreenshotParams,
) {
    navController.navigate("${ArticleAppScreen.Screenshot.name}/${params.articleType}/${params.articleId}?imageUrl=${params.imageUrl}")
}

private fun navigateToAiAudio(
    navController: NavController
) {
    navController.navigate(ArticleAppScreen.Audio.name)
}
