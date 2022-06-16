package com.ft.ftchinese.ui.main

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.*
import com.ft.ftchinese.model.content.ChannelSource
import com.ft.ftchinese.model.content.HTML_TYPE_FRAGMENT
import com.ft.ftchinese.model.content.RemoteMessageType
import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.model.enums.ArticleType
import com.ft.ftchinese.service.SplashWorker
import com.ft.ftchinese.ui.article.ArticleActivity
import com.ft.ftchinese.ui.article.ChannelActivity
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.main.splash.SplashActivityScreen
import com.ft.ftchinese.ui.main.terms.TermsActivityScreen
import com.ft.ftchinese.ui.theme.OTheme

private const val EXTRA_MESSAGE_TYPE = "content_type"
private const val EXTRA_CONTENT_ID = "content_id"
private const val TAG = "SplashActivity"

class SplashActivity : ScopedAppActivity() {

    private lateinit var workManager: WorkManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SplashApp(
                onNext = this::exit,
                onDeclined = {
                    finish()
                }
            )
        }

        workManager = WorkManager.getInstance(this)
        handleMessaging()
    }

    private fun handleMessaging() {
        // FCM delivers data to the `intent` as key-value pairs,
        // including your custom data, when the app is in background.
        // When app is in foreground, data will be delivered to
        // NewsMessagingService.
        //
        // FCM standard keys:
        // google.delivered_priority: high
        // google.sent_time: 1565331193712
        // google.ttl Value: 2419200
        // from: It seems this is a fixed numeric id.
        // google.message_id:
        // collapse_key: com.ft.ftchinese
        //
        // Following are custom data fields
        // contentType: story | video | photo | interactive
        // contentId: 001084989
        intent.extras?.let {
            for (key in it.keySet()) {
                val value = intent.extras?.get(key)
                Log.i(TAG, "$key: $value")
            }
        }

        // Receive message in the background.
        // Those data are attached in the Customer data section of FCM composer.
        // They are key-value pairs.
        // We use `contentType` as key for EXTRA_MESSAGE_TYPE
        // and use `contentId` as key for article's id.
        val msgTypeStr = intent.getStringExtra(EXTRA_MESSAGE_TYPE) ?: return
        val msgType = RemoteMessageType.fromString(msgTypeStr) ?: return

        val pageId = intent.getStringExtra(EXTRA_CONTENT_ID) ?: return

        Log.i(TAG, "Message type: $msgType, content id: $pageId")

        val contentType = msgType.toArticleType() ?: return
        when (msgType) {
            RemoteMessageType.Story,
            RemoteMessageType.Video,
            RemoteMessageType.Photo,
            RemoteMessageType.Interactive -> {

                ArticleActivity.startWithParentStack(this, Teaser(
                    id = pageId,
                    type = ArticleType.fromString(contentType),
                    title = ""
                ))
            }

            RemoteMessageType.Tag,
            RemoteMessageType.Channel -> {
                ChannelActivity.startWithParentStack(this, ChannelSource(
                    title = pageId,
                    name = "${msgType}_$pageId",
                    path = "",
                    query = "",
                    htmlType = HTML_TYPE_FRAGMENT
                ))
            }

        }
    }

    private fun setupWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresBatteryNotLow(true)
            .build()

        val splashWork = OneTimeWorkRequestBuilder<SplashWorker>()
            .setConstraints(constraints)
            .build()

        workManager
            .enqueueUniqueWork(
                "nextRoundSplash",
                ExistingWorkPolicy.REPLACE,
                splashWork,
            )

        workManager.getWorkInfoByIdLiveData(splashWork.id).observe(this) {
            Log.i(TAG, "splashWork status ${it.state}")
        }
    }


    // NOTE: it must be called in the main thread.
    // If you call is in a non-Main coroutine, it crashes.
    private fun exit() {
        Log.i(TAG, "Exiting Splash Activity")
        MainActivity.start(this)
        setupWorker()
        finish()
    }
}

@Composable
fun SplashApp(
    onNext: () -> Unit,
    onDeclined: () -> Unit
) {
    val scaffoldState = rememberScaffoldState()
    val navController = rememberNavController()

    OTheme {
        Scaffold(
            scaffoldState = scaffoldState
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screens.Splash.name,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(
                    route = Screens.Splash.name
                ) {
                    SplashActivityScreen(
                        onAgreement = {
                            navController
                                .navigate(Screens.Terms.name)
                        },
                        onNext = onNext
                    )
                }

                composable(
                    route = Screens.Terms.name
                ) {
                    TermsActivityScreen(
                        onAgreed = onNext,
                        onDeclined = onDeclined
                    )
                }
            }
        }
    }
}

private enum class Screens {
    Splash,
    Terms;
}
