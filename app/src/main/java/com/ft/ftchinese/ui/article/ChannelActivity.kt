package com.ft.ftchinese.ui.article

import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ft.ftchinese.R
import com.ft.ftchinese.model.content.ChannelSource
import com.ft.ftchinese.ui.article.audio.AiAudioActivityScreen
import com.ft.ftchinese.ui.article.chl.ChannelActivityScreen
import com.ft.ftchinese.ui.article.content.ArticleActivityScreen
import com.ft.ftchinese.ui.article.screenshot.ScreenshotActivityScreen
import com.ft.ftchinese.ui.util.toast
import com.ft.ftchinese.ui.components.Toolbar
import com.ft.ftchinese.ui.theme.OTheme
import com.google.firebase.analytics.FirebaseAnalytics

private const val EXTRA_CHANNEL_SOURCE = "extra_channel_source"
private const val TAG = "ChannelActivity"

/**
 * This is used to show a channel page, which consists of a list of article teaser.
 * It is similar to MainActivity except that it does not wrap a TabLayout.
 * Use cases: column channel, editor's choice, archive list.
 */
class ChannelActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val channelSource = intent.getParcelableExtra<ChannelSource>(EXTRA_CHANNEL_SOURCE)
        if (channelSource == null) {
            toast(R.string.loading_failed)
            return
        }

        val id = NavStore.saveChannel(channelSource)

        setContent {
            ChannelApp(
                initialId = id,
                onExit = { finish() }
            )
        }
        FirebaseAnalytics.getInstance(this)
            .logEvent(
                    FirebaseAnalytics.Event.VIEW_ITEM_LIST, Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_CATEGORY, channelSource.title
            )
        })
    }

    /**
     * Launch this activity with intent
     */
    companion object {

        @JvmStatic
        fun newIntent(context: Context?, teaser: ChannelSource): Intent {
            return Intent(
                    context,
                    ChannelActivity::class.java
            ).apply {
                putExtra(EXTRA_CHANNEL_SOURCE, teaser)
            }
        }

        /**
         * Start [ChannelActivity] based on values passed from JS.
         */
        @JvmStatic
        fun start(context: Context?, page: ChannelSource) {
            val intent = Intent(context, ChannelActivity::class.java).apply {
                putExtra(EXTRA_CHANNEL_SOURCE, page)
            }

            context?.startActivity(intent)
        }

        @JvmStatic
        fun startWithParentStack(context: Context, page: ChannelSource) {
            val intent = Intent(context, ChannelActivity::class.java).apply {
                putExtra(EXTRA_CHANNEL_SOURCE, page)
            }

            TaskStackBuilder
                .create(context)
                .addNextIntentWithParentStack(intent)
                .startActivities()
        }
    }
}

@Composable
fun ChannelApp(
    initialId: String,
    onExit: () -> Unit
) {
    val scaffoldState = rememberScaffoldState()
    val navController = rememberNavController()
    val backstackEntry = navController.currentBackStackEntryAsState()
    val currentScreen = ArticleAppScreen.fromRoute(
        backstackEntry.value?.destination?.route
    )

    val (title, setTitle) = remember {
        mutableStateOf("")
    }

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
                startDestination = "${ArticleAppScreen.Channel.name}/{id}",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(
                    route = "${ArticleAppScreen.Channel.name}/{id}",
                    arguments = listOf(
                        navArgument("id") {
                            type = NavType.StringType
                            defaultValue = initialId
                        }
                    )
                ) { entry ->
                    val id = entry.arguments?.getString("id")
                    ChannelActivityScreen(
                        scaffoldState = scaffoldState,
                        id = id,
                        onArticle = {
                            Log.i(TAG, "Open an article $it")
                            navigate(
                                navController = navController,
                                screen = ArticleAppScreen.Story,
                                id = it
                            )
                        },
                        onChannel = {
                            Log.i(TAG, "Open another channel")
                            navigate(
                                navController = navController,
                                screen = ArticleAppScreen.Channel,
                                it,
                            )
                        }
                    )
                }

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
                                id = it
                            )
                        },
                        onChannel = {
                            navigate(
                                navController = navController,
                                screen = ArticleAppScreen.Channel,
                                it,
                            )
                        },
                        onArticle = {
                            navigate(
                                navController = navController,
                                screen = ArticleAppScreen.Story,
                                it,
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
