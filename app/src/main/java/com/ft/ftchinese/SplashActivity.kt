package com.ft.ftchinese

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.ViewModelProviders
import com.ft.ftchinese.base.ScopedAppActivity
import com.ft.ftchinese.model.SessionManager
import com.ft.ftchinese.model.StatsTracker
import com.ft.ftchinese.model.splash.SplashScreenManager
import com.ft.ftchinese.ui.splash.SplashViewModel
import com.ft.ftchinese.util.FileCache
import kotlinx.android.synthetic.main.activity_splash.*
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger

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
        splashViewModel = ViewModelProviders.of(this)
                .get(SplashViewModel::class.java)

        statsTracker = StatsTracker.getInstance(this)

        initUI()
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

        val drawable = cache.readDrawable(imageName)

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

        counterJob = GlobalScope.launch(Dispatchers.Main) {

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
