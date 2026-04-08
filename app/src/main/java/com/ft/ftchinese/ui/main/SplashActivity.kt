package com.ft.ftchinese.ui.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.*
import com.ft.ftchinese.service.PushNotificationRouter
import com.ft.ftchinese.service.SplashWorker
import com.ft.ftchinese.ui.main.splash.SplashActivityScreen
import com.ft.ftchinese.ui.main.terms.TermsActivityScreen
import com.ft.ftchinese.ui.theme.OTheme
private const val TAG = "SplashActivity"

class SplashActivity : AppCompatActivity() {

    private lateinit var workManager: WorkManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        workManager = WorkManager.getInstance(this)
        if (handleNotificationIntent(intent, source = "onCreate")) {
            finish()
            return
        }

        setContent {
            SplashApp(
                onNext = this::exit,
                onDeclined = {
                    finish()
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (handleNotificationIntent(intent, source = "onNewIntent")) {
            finish()
        }
    }

    private fun handleNotificationIntent(launchIntent: Intent?, source: String): Boolean {
        if (launchIntent == null) {
            return false
        }

        launchIntent.extras?.let {
            for (key in it.keySet()) {
                val value = launchIntent.extras?.get(key)
                Log.i(TAG, "notification_intent[$source] $key: $value")
            }
        }

        return runCatching {
            val route = PushNotificationRouter.routeFromIntent(launchIntent) ?: return false
            val useParentStack = launchIntent.getStringExtra(PushNotificationRouter.EXTRA_LAUNCH_MODE) !=
                PushNotificationRouter.LAUNCH_MODE_DIRECT
            PushNotificationRouter.start(
                this,
                route,
                source,
                useParentStack = useParentStack,
            )
            true
        }.getOrElse { error ->
            Log.e(TAG, "notification_intent[$source] failed message=${error.message}", error)
            MainActivity.start(this)
            true
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
