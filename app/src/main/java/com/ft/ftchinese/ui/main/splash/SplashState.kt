package com.ft.ftchinese.ui.main.splash

import android.content.Context
import android.util.Log
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.model.request.WxMiniParams
import com.ft.ftchinese.model.splash.ScreenAd
import com.ft.ftchinese.repository.AdClient
import com.ft.ftchinese.store.FileStore
import com.ft.ftchinese.store.SplashStore
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.ui.util.ConnectionState
import com.ft.ftchinese.ui.util.connectivityState
import com.ft.ftchinese.ui.components.BaseState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "SplashState"

data class SplashShown(
    val image: File,
    val ad: ScreenAd,
)

class SplashState(
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
    connState: State<ConnectionState>,
    context: Context,
) : BaseState(scaffoldState, scope, context.resources, connState) {

    private val totalTime = 5

    private val fileStore = FileStore(context)
    private val splashStore = SplashStore.getInstance(context)
    private val tracker = StatsTracker.getInstance(context)
    private var job: Job? = null

    var currentTime by mutableStateOf(-1)
        private set

    var shouldExit by mutableStateOf(false)
        private set

    var splashShown by mutableStateOf<SplashShown?>(null)
        private set

    private fun startCounting() {
        job = scope.launch {
            for (i in totalTime downTo 1) {
                currentTime = i
                delay(1000)
            }
            shouldExit = true
        }
    }

    fun stopCounting() {
        job?.cancel()
    }

    fun skip() {
        if (currentTime > 0) {
            splashShown?.let {
                tracker.adSkipped(it.ad)
            }
        }

        job?.cancel()
        shouldExit = true
    }

    fun initLoading() {
        scope.launch {
            val splashAd = splashStore.load()
            if (splashAd == null) {
                Log.i(TAG, "Splash not found. Exit")
                shouldExit = true
                return@launch
            }

            Log.i(TAG, "Splash found $splashAd")

            if (!splashAd.isToday() || splashAd.isVideo) {
                Log.i(TAG, "Splash is not targeting today. Exit")
                shouldExit = true
                return@launch
            }

            val imageName = splashAd.imageName
            if (imageName == null) {
                Log.i(TAG, "Splash has no image name. Exit")
                shouldExit = true
                return@launch
            }

            val file = fileStore.newfile(imageName)
            val exists = try {
                file.exists()
            } catch (e: Exception) {
                false
            }
            if (!exists) {
                Log.i(TAG, "Splash image $imageName not found")
                shouldExit = true
                return@launch
            }

            splashShown = SplashShown(
                image = file,
                ad = splashAd
            )

            startCounting()

            // Tracking
            tracker.adViewed(screenAd = splashAd)
            splashAd.impressionDest().forEach { url ->
                sendImpression(url)
            }
        }
    }

    private suspend fun sendImpression(url: String) {
        tracker.launchAdSent(url)

        val result = AdClient.asyncSendImpression(url)

        when {
            result.isFailure -> {
                result.exceptionOrNull()?.message?.let { Log.i(TAG, it) }
                tracker.launchAdSuccess(url)
            }
            result.isSuccess -> {
                tracker.launchAdSuccess(url)
            }
        }
    }

    fun trackAdClicked() {
        splashShown?.let {
            tracker.adClicked(it.ad)
        }
    }

    fun trackWxMini(params: WxMiniParams) {
        tracker.openedInWxMini(params)
    }
}

@Composable
fun rememberSplashState(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    connState: State<ConnectionState> = connectivityState(),
    scope: CoroutineScope = rememberCoroutineScope(),
    context: Context = LocalContext.current,
) = remember(scaffoldState, connState) {
    SplashState(
        scaffoldState = scaffoldState,
        scope = scope,
        connState = connState,
        context = context
    )
}
