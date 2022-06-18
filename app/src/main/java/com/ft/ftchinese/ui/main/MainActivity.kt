package com.ft.ftchinese.ui.main

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.work.*
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityMainBinding
import com.ft.ftchinese.model.content.WebpageMeta
import com.ft.ftchinese.service.LatestReleaseWorker
import com.ft.ftchinese.service.VerifySubsWorker
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.model.content.TabPages
import com.ft.ftchinese.ui.util.toast
import com.ft.ftchinese.ui.channel.TabPagerAdapter
import com.ft.ftchinese.ui.myft.MyftPagerAdapter
import com.ft.ftchinese.ui.main.search.SearchableActivity
import com.ft.ftchinese.ui.webpage.WebpageActivity
import com.ft.ftchinese.viewmodel.UserViewModel
import com.google.android.material.tabs.TabLayout
import com.tencent.mm.opensdk.openapi.WXAPIFactory

/**
 * MainActivity implements ChannelFragment.OnFragmentInteractionListener to interact with TabLayout.
 */
class MainActivity : AppCompatActivity(),
        TabLayout.OnTabSelectedListener {

    @Deprecated("")
    private var pagerAdapter: TabPagerAdapter? = null

    @Deprecated("")
    private lateinit var binding: ActivityMainBinding

    @Deprecated("")
    private lateinit var sessionManager: SessionManager

    @Deprecated("")
    private lateinit var statsTracker: StatsTracker

    private lateinit var conversionViewModel: ConversionViewModel
    private lateinit var userViewModel: UserViewModel

    private val startForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            toast(R.string.login_success)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: remove
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_main,
        )
        // TODO: remove
        setSupportActionBar(binding.toolbar)

        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        registerWx()

        // TODO: set compose ui.


        createNotificationChannel()

        setupWorker()
        setupConversion()

        statsTracker = StatsTracker.getInstance(this)
        sessionManager = SessionManager.getInstance(this)

        // Set ViewPager adapter
        setupHome()

        // Link ViewPager and TabLayout
        binding.tabLayout.setupWithViewPager(binding.viewPager)
        binding.tabLayout.addOnTabSelectedListener(this)

        setupBottomNav()

        statsTracker.appOpened()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = getString(R.string.news_notification_channel_id)
            val channelName = getString(R.string.news_notification_channel_name)
            val channelDesc = getString(R.string.news_notification_channel_description)

            val channel = NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = channelDesc
            }
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun setupWorker() {
        val workManager = WorkManager.getInstance(this)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresBatteryNotLow(true)
            .build()

        val verifyWork = OneTimeWorkRequestBuilder<VerifySubsWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork("verifySubscription", ExistingWorkPolicy.REPLACE, verifyWork)

        workManager.getWorkInfoByIdLiveData(verifyWork.id).observe(this) { workInfo ->
            Log.i(TAG, "verifyWork state ${workInfo.state}")
        }

        val upgradeWork = OneTimeWorkRequestBuilder<LatestReleaseWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork("latestRelease", ExistingWorkPolicy.REPLACE, upgradeWork)
    }

    // Register Wechat id
    private fun registerWx() {
        WXAPIFactory.createWXAPI(
            this,
            BuildConfig.WX_SUBS_APPID, false
        ).apply {
            registerApp(BuildConfig.WX_SUBS_APPID)
        }
    }

    private fun setupConversion() {
        conversionViewModel = ViewModelProvider(this)[ConversionViewModel::class.java]
        // Open conversion tracking page.
        conversionViewModel.campaignLiveData.observe(this) {
            WebpageActivity.start(
                context = this,
                meta = WebpageMeta(
                    title = "",
                    url = it.url,
                    showMenu = false,
                )
            )
        }
        conversionViewModel.launchTask(3, 30, 7)
    }

    @Deprecated("")
    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener {
            Log.i(TAG, "Selected bottom nav item ${it.title}")

            when (it.itemId) {
                R.id.nav_news -> {
                    setupHome()
                }

                R.id.nav_english -> {
                    pagerAdapter = TabPagerAdapter(TabPages.englishPages, supportFragmentManager)
                    binding.viewPager.adapter = pagerAdapter

                    displayTitle(R.string.nav_english)
                }

                R.id.nav_ftacademy -> {
                    pagerAdapter = TabPagerAdapter(TabPages.ftaPages, supportFragmentManager)
                    binding.viewPager.adapter = pagerAdapter

                    displayTitle(R.string.nav_ftacademy)
                }

                R.id.nav_video -> {
                    pagerAdapter = TabPagerAdapter(TabPages.videoPages, supportFragmentManager)
                    binding.viewPager.adapter = pagerAdapter

                    displayTitle(R.string.nav_video)
                }

                R.id.nav_myft -> {
                    binding.viewPager.adapter = MyftPagerAdapter(supportFragmentManager)
                    pagerAdapter = null

                    displayTitle(R.string.nav_myft)
                }
            }
            true
        }
    }

    /**
     * Set up home page upon launching, or clicking the Home button in BottomNavigationView.
     */
    @Deprecated("")
    private fun setupHome() {
        pagerAdapter = TabPagerAdapter(TabPages.newsPages, supportFragmentManager)
        binding.viewPager.adapter = pagerAdapter

        supportActionBar?.apply {
            setDisplayUseLogoEnabled(true)
            setDisplayShowTitleEnabled(false)
            setLogo(R.drawable.ic_menu_masthead)
        }
    }

    @Deprecated("")
    private fun displayTitle(title: Int) {
        supportActionBar?.apply {
            setDisplayUseLogoEnabled(false)
            setDisplayShowTitleEnabled(true)
            setTitle(title)
        }
    }

    /**
     * Create menus on toolbar
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_main_search, menu)
        return true
    }

    /**
     * Respond to menu item on the toolbar being selected
     */
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
            R.id.action_search -> {
                SearchableActivity.start(this)
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }

    /**
     * Implementation of TabLayout.OnTabSelectedListener
     * Tab index starts from 0
     */
    override fun onTabSelected(tab: TabLayout.Tab?) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "Tab selected: ${tab?.position}")
        }

        val position = tab?.position ?: return
        val title = pagerAdapter?.getPageTitle(position) ?: return

        if (BuildConfig.DEBUG) {
            Log.i(TAG, "View item list event: $title")
        }

        statsTracker.tabSelected(title.toString())
    }

    override fun onTabReselected(tab: TabLayout.Tab?) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "Tab reselected: ${tab?.position}")
        }
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "Tab unselected: ${tab?.position}")
        }
    }

    companion object {
        private const val TAG = "MainActivity"

        @JvmStatic
        fun start(context: Context) {
            context.startActivity(Intent(context, MainActivity::class.java))
        }
    }
}


