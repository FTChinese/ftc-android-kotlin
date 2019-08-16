package com.ft.ftchinese.ui.launch

import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.model.*
import com.ft.ftchinese.model.reader.SessionManager
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.model.splash.SplashScreenManager
import com.ft.ftchinese.ui.article.ArticleActivity
import com.ft.ftchinese.ui.channel.ChannelActivity
import com.ft.ftchinese.util.FileCache
import kotlinx.android.synthetic.main.activity_splash.*
import kotlinx.android.synthetic.main.activity_splash.ad_image
import kotlinx.android.synthetic.main.activity_splash.ad_timer
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

private const val EXTRA_MESSAGE_TYPE = "action"
private const val EXTRA_CONTENT_ID = "pageId"

@kotlinx.coroutines.ExperimentalCoroutinesApi
class SplashActivity : ScopedAppActivity(), AnkoLogger {

    private lateinit var cache: FileCache
    private lateinit var sessionManager: SessionManager
    private lateinit var statsTracker: StatsTracker
    private lateinit var splashManager: SplashScreenManager
    private lateinit var splashViewModel: SplashViewModel
    private var counterJob: Job? = null
    private var customTabsOpened: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        cache = FileCache(this)
        sessionManager = SessionManager.getInstance(this)
        splashManager = SplashScreenManager(this)
        splashViewModel = ViewModelProvider(this)
                .get(SplashViewModel::class.java)

        statsTracker = StatsTracker.getInstance(this)

        initUI()

        // google.delivered_priority: high
        // google.sent_time: 1565331193712
        // google.ttl Value: 2419200
        // from:
        // google.message_id:
        // collapse_key:
        intent.extras?.let {
            for (key in it.keySet()) {
                val value = intent.extras?.get(key)
                info("Key: $key Value: $value")
            }
        }

        val msgTypeStr = intent.getStringExtra(EXTRA_MESSAGE_TYPE) ?: return
        val msgType = RemoteMessageType.fromString(msgTypeStr) ?: return

        val pageId = intent.getStringExtra(EXTRA_CONTENT_ID) ?: return

        info("Message type: $msgType, content id: $pageId")

        when (msgType) {
            RemoteMessageType.Story -> {

                ArticleActivity.start(this, ChannelItem(
                        id = pageId,
                        type = "story",
                        title = ""
                ))
            }
            RemoteMessageType.Video -> {
                ArticleActivity.startWeb(this, ChannelItem(
                        id = pageId,
                        type = "video",
                        title = ""
                ))
            }
            RemoteMessageType.Photo -> {
                ArticleActivity.startWeb(this, ChannelItem(
                        id = pageId,
                        type = "photonews",
                        title = ""
                ))
            }
            RemoteMessageType.Academy -> {
                ArticleActivity.startWeb(this, ChannelItem(
                        id = pageId,
                        type = "interactive",
                        title = ""
                ))
            }
            RemoteMessageType.SpecialReport -> {

            }
            RemoteMessageType.Tag -> {
                ChannelActivity.start(this, ChannelSource(
                        title = pageId,
                        name = "${msgType}_$pageId",
                        contentUrl = "",
                        htmlType = HTML_TYPE_FRAGMENT
                ))
            }
            RemoteMessageType.Channel -> {
                ChannelActivity.start(this, ChannelSource(
                        title = pageId,
                        name = "${msgType}_$pageId",
                        contentUrl = "",
                        htmlType = HTML_TYPE_FRAGMENT

                ))
            }
            RemoteMessageType.Other -> {

            }
            RemoteMessageType.Download -> {

            }
        }

        counterJob?.cancel()
        showSystemUI()
        finish()
    }

    private fun initUI() {
        ad_timer.visibility = View.GONE

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

        ad_timer.setOnClickListener {
            statsTracker.adSkipped(splashAd)
            counterJob?.cancel()

            exit()
            return@setOnClickListener
        }

        ad_image.setOnClickListener {
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

            ad_image.setImageDrawable(drawable)
            ad_timer.visibility = View.VISIBLE

            for (i in 5 downTo  1) {
                ad_timer.text = getString(R.string.prompt_ad_timer, i)
                delay(1000)
            }

            exit()
        }
    }

    // NOTE: it must be called in the main thread.
    // If you call is in a non-Main coroutine, it crashes.
    private fun exit() {
        ad_timer.visibility = View.GONE
        ad_image.visibility = View.GONE

        showSystemUI()
        MainActivity.start(this)
        finish()
    }

    private fun hideSystemUI() {
        ad_container.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }

    private fun showSystemUI() {
        ad_container.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }

    override fun onResume() {
        super.onResume()

        if (customTabsOpened) {
            exit()
        }
    }
}
