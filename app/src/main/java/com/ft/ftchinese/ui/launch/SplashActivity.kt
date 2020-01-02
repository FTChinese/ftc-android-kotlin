package com.ft.ftchinese.ui.launch

import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.browser.customtabs.CustomTabsIntent
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivitySplashBinding
import com.ft.ftchinese.model.*
import com.ft.ftchinese.model.reader.SessionManager
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.model.splash.SplashScreenManager
import com.ft.ftchinese.ui.article.ArticleActivity
import com.ft.ftchinese.ui.channel.ChannelActivity
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.viewmodel.SplashViewModel
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

private const val EXTRA_MESSAGE_TYPE = "content_type"
private const val EXTRA_CONTENT_ID = "content_id"

@kotlinx.coroutines.ExperimentalCoroutinesApi
class SplashActivity : ScopedAppActivity(), AnkoLogger {

    private lateinit var cache: FileCache
    private lateinit var sessionManager: SessionManager
    private lateinit var statsTracker: StatsTracker
    private lateinit var splashManager: SplashScreenManager
    private lateinit var splashViewModel: SplashViewModel
    private var counterJob: Job? = null
    private var customTabsOpened: Boolean = false
    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_splash)

        cache = FileCache(this)
        sessionManager = SessionManager.getInstance(this)
        splashManager = SplashScreenManager(this)
        splashViewModel = ViewModelProvider(this)
                .get(SplashViewModel::class.java)

        statsTracker = StatsTracker.getInstance(this)

        initUI()

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
                info("$key: $value")
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

        info("Message type: $msgType, content id: $pageId")

        val contentType = msgType.toArticleType() ?: return
        when (msgType) {
            RemoteMessageType.Story,
            RemoteMessageType.Video,
            RemoteMessageType.Photo,
            RemoteMessageType.Interactive -> {

                ArticleActivity.startWithParentStack(this, Teaser(
                        id = pageId,
                        type = contentType,
                        title = ""
                ))
            }

            RemoteMessageType.Tag,
            RemoteMessageType.Channel -> {
                ChannelActivity.startWithParentStack(this, ChannelSource(
                        title = pageId,
                        name = "${msgType}_$pageId",
                        contentUrl = "",
                        htmlType = HTML_TYPE_FRAGMENT
                ))
            }

        }

        counterJob?.cancel()
        showSystemUI()
        finish()
    }

    private fun initUI() {

        val splashAd = splashManager.load()

        if (splashAd == null) {
            exit()
            return
        }

        if (!splashAd.isToday()) {
            exit()
            return
        }

        val imageName = splashAd.imageName
        if (imageName == null) {
            exit()
            return
        }

        if (!cache.exists(imageName)) {
            exit()
            return
        }

        val drawable = Drawable.createFromStream(
                openFileInput(imageName),
                imageName
        )

        if (drawable == null) {
            exit()
            return
        }

        binding.adTimer.setOnClickListener {
            statsTracker.adSkipped(splashAd)
            counterJob?.cancel()

            exit()
            return@setOnClickListener
        }

        binding.adImage.setOnClickListener {
            statsTracker.adClicked(splashAd)
            counterJob?.cancel()

            customTabsOpened = true

            CustomTabsIntent
                    .Builder()
                    .build()
                    .launchUrl(
                            this@SplashActivity,
                            Uri.parse(splashAd.linkUrl)
                    )

            return@setOnClickListener
        }

        counterJob = launch(Dispatchers.Main) {

            splashViewModel.sendImpression(splashAd, statsTracker)

            statsTracker.adViewed(splashAd)

            hideSystemUI()

            binding.adImage.setImageDrawable(drawable)

            for (i in 5 downTo  1) {
                binding.timerText = getString(R.string.prompt_ad_timer, i)
                delay(1000)
            }

            exit()
        }
    }

    // NOTE: it must be called in the main thread.
    // If you call is in a non-Main coroutine, it crashes.
    private fun exit() {
        binding.timerText = null

        showSystemUI()
        MainActivity.start(this)
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
