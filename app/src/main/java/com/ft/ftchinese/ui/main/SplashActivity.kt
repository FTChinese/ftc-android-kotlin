package com.ft.ftchinese.ui.main

import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.browser.customtabs.CustomTabsIntent
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.work.*
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivitySplashBinding
import com.ft.ftchinese.model.content.*
import com.ft.ftchinese.model.splash.SplashScreenManager
import com.ft.ftchinese.service.SplashWorker
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.ui.article.ArticleActivity
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.ui.channel.ChannelActivity
import kotlinx.coroutines.*

private const val EXTRA_MESSAGE_TYPE = "content_type"
private const val EXTRA_CONTENT_ID = "content_id"
private const val TAG = "SplashActivity"

@kotlinx.coroutines.ExperimentalCoroutinesApi
class SplashActivity : ScopedAppActivity() {

    private lateinit var cache: FileCache
    private lateinit var sessionManager: SessionManager
    private lateinit var statsTracker: StatsTracker
    private lateinit var splashManager: SplashScreenManager
    private lateinit var splashViewModel: SplashViewModel
    // Why this?
    private var customTabsOpened: Boolean = false
    private lateinit var binding: ActivitySplashBinding
    private lateinit var workManager: WorkManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_splash)

        cache = FileCache(this)
        sessionManager = SessionManager.getInstance(this)
        splashManager = SplashScreenManager.getInstance(this)
        statsTracker = StatsTracker.getInstance(this)

        workManager = WorkManager.getInstance(this)

        splashViewModel = ViewModelProvider(this)
            .get(SplashViewModel::class.java)

        connectionLiveData.observe(this) {
            splashViewModel.isNetworkAvailable.value = it
        }

        isConnected.let {
            splashViewModel.isNetworkAvailable.value = it
        }

        binding.handler = this

        setupViewModel()
        splashViewModel.loadAd(splashManager, cache)
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

    private fun setupViewModel() {
        splashViewModel.shouldExit.observe(this) {
            if (it) {
                Log.i(TAG, "shouldExit is $it")
                exit()
            }
        }

        splashViewModel.adLoaded.observe(this) {
            Log.i(TAG, "Splash loaded $it")
            // Tracking event ad viewed.
            statsTracker.adViewed(it)
            // Send a request to the tracking url defined in the ad
            // data.
            it.impressionDest().forEach { url ->
                statsTracker.launchAdSent(url)
                splashViewModel.sendImpression(url)
            }
        }

        // Show the image.
        splashViewModel.imageLoaded.observe(this) {
            Log.i(TAG, "Splash image loaded. Creating drawable")
            val drawable = Drawable.createFromStream(
                it.first,
                it.second,
            )

            if (drawable == null) {
                Log.i(TAG, "Create drawable failed.")
                exit()
                return@observe
            }

            hideSystemUI()
            binding.adImage.setImageDrawable(drawable)
            splashViewModel.startCounting()
        }

        // Tracking if impression is successfully sent.
        splashViewModel.impressionResult.observe(this) {
            if (it.first) {
                statsTracker.launchAdSuccess(it.second)
            } else {
                statsTracker.launchAdFail(it.second)
            }
        }

        // Timer text.
        splashViewModel.counterLiveData.observe(this) {
            binding.timerText = getString(R.string.prompt_ad_timer, it)
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

    // When clicking counter, skip the ad.
    fun onClickCounter(view: View) {
        splashViewModel.adLoaded.value?.let {
            statsTracker.adSkipped(it)
        }
        splashViewModel.stopCounting()
        exit()
    }

    fun onClickImage(view: View) {
        // Stop counting and exit
        splashViewModel.stopCounting()
        splashViewModel.adLoaded.value?.let {
            // Chrome web opened on top of this activity to show ad.
            // After user exited from ad, the onResume method is called and if customTabsOpened is true, it will
            // call exit().
            customTabsOpened = true
            statsTracker.adClicked(it)

            CustomTabsIntent
                .Builder()
                .build()
                .launchUrl(
                    this,
                    Uri.parse(it.linkUrl)
                )
        }
    }

    // NOTE: it must be called in the main thread.
    // If you call is in a non-Main coroutine, it crashes.
    private fun exit(showAd: Boolean = false) {
        Log.i(TAG, "Exiting Splash Activity")
        showSystemUI()
        MainActivity.start(this)
        setupWorker()
        finish()
    }

    private fun hideSystemUI() {
        binding.adContainer.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }

    private fun showSystemUI() {
        binding.adContainer.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }

    override fun onResume() {
        super.onResume()

        if (customTabsOpened) {
            exit()
        }
    }
}
