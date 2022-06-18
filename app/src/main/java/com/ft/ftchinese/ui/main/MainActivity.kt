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
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.work.*
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityMainBinding
import com.ft.ftchinese.model.enums.LoginMethod
import com.ft.ftchinese.model.legal.WebpageMeta
import com.ft.ftchinese.service.LatestReleaseWorker
import com.ft.ftchinese.service.VerifySubsWorker
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.TabPages
import com.ft.ftchinese.ui.base.toast
import com.ft.ftchinese.ui.channel.TabPagerAdapter
import com.ft.ftchinese.ui.dialog.WxExpireDialogFragment
import com.ft.ftchinese.ui.myft.MyftPagerAdapter
import com.ft.ftchinese.ui.search.SearchableActivity
import com.ft.ftchinese.ui.webpage.WebpageActivity
import com.google.android.material.tabs.TabLayout
import com.stripe.android.CustomerSession
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * MainActivity implements ChannelFragment.OnFragmentInteractionListener to interact with TabLayout.
 */
class MainActivity : ScopedAppActivity(),
        TabLayout.OnTabSelectedListener {

    private var mBackKeyPressed = false
    private var pagerAdapter: TabPagerAdapter? = null

    private lateinit var conversionViewModel: ConversionViewModel

    private lateinit var binding: ActivityMainBinding
//    private lateinit var navHeaderBinding: DrawerNavHeaderBinding

    private lateinit var sessionManager: SessionManager
    private lateinit var workManager: WorkManager

    private lateinit var statsTracker: StatsTracker

    private val startForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            toast(R.string.login_success)
            updateSessionUI()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_main,
        )
        setSupportActionBar(binding.toolbar)

        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        createNotificationChannel()

        statsTracker = StatsTracker.getInstance(this)

        // Register Wechat id
        WXAPIFactory.createWXAPI(
            this,
            BuildConfig.WX_SUBS_APPID, false
        ).apply {
            registerApp(BuildConfig.WX_SUBS_APPID)
        }

        sessionManager = SessionManager.getInstance(this)
        workManager = WorkManager.getInstance(this)

        conversionViewModel = ViewModelProvider(this)[ConversionViewModel::class.java]

        setupViewModel()

        // Set ViewPager adapter
        setupHome()

        // Link ViewPager and TabLayout
        binding.tabLayout.setupWithViewPager(binding.viewPager)
        binding.tabLayout.addOnTabSelectedListener(this)

        setupBottomNav()
//        setupDrawer()

        statsTracker.appOpened()

        checkWxSession()

        setupWorker()
    }

    private fun setupViewModel() {

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
    private fun setupHome() {
        pagerAdapter = TabPagerAdapter(TabPages.newsPages, supportFragmentManager)
        binding.viewPager.adapter = pagerAdapter

        supportActionBar?.apply {
            setDisplayUseLogoEnabled(true)
            setDisplayShowTitleEnabled(false)
            setLogo(R.drawable.ic_menu_masthead)
        }
    }

    private fun displayTitle(title: Int) {
        supportActionBar?.apply {
            setDisplayUseLogoEnabled(false)
            setDisplayShowTitleEnabled(true)
            setTitle(title)
        }
    }

    /**
     * Add event listener to drawer menu.
     */
//    private fun setupDrawer() {
//
//        navHeaderBinding = DataBindingUtil.inflate(
//            layoutInflater,
//            R.layout.drawer_nav_header,
//            binding.drawerNav,
//            false)
//
//        binding.drawerNav.apply {
//            addHeaderView(navHeaderBinding.root)
//            // Hide test section
//            menu.setGroupVisible(R.id.drawer_group3, BuildConfig.DEBUG)
//        }
//
//        // Set a listener that will be notified when a menu item is selected.
//        binding.drawerNav.setNavigationItemSelectedListener {
//            when (it.itemId) {
//                R.id.action_login -> {
//                    startForResult.launch(
//                        AuthActivity.newIntent(this)
//                    )
//                }
//                R.id.action_account -> AccountActivity.start(this)
//                R.id.action_paywall -> {
//                    // Tracking
//                    PaywallTracker.fromDrawer()
//                    SubsActivity.start(this)
//                }
//                R.id.action_my_subs -> MemberActivity.start(this)
//                R.id.action_settings -> SettingsActivity.start(this)
//                R.id.action_test -> TestActivity.start(this)
//            }
//
//            binding.drawerLayout.closeDrawer(GravityCompat.START)
//
//            true
//        }
//
//        ActionBarDrawerToggle(
//            this,
//            binding.drawerLayout,
//            binding.toolbar,
//            R.string.navigation_drawer_open,
//            R.string.navigation_drawer_close
//        ).apply {
//            binding.drawerLayout.addDrawerListener(this)
//            syncState()
//        }
//
//        updateSessionUI()
//    }

    /**
     * Update UI depending on user's login/logout state
     */
    private fun updateSessionUI() {

        val account = sessionManager.loadAccount()

//        navHeaderBinding.account = account

//        binding.drawerNav.menu.apply {
//            // show signin/signup if account is null.
//            setGroupVisible(R.id.drawer_group_sign_in_up, account == null)
//            // Show account.
//            findItem(R.id.action_account)?.isVisible = account != null
//            // Only show when user is a member.
//            findItem(R.id.action_my_subs)?.isVisible = account?.isMember ?: false
//            findItem(R.id.action_paywall)?.isVisible = !(account?.isMember ?: false)
//        }
    }

    private fun logout() {
        sessionManager.logout()
        CustomerSession.endCustomerSession()
        updateSessionUI()
        toast("账号已登出")
    }



    /**
     * Check whether wechat session has expired.
     * Wechat refresh token expires after 30 days.
     */
    private fun checkWxSession() {
        val account = sessionManager.loadAccount() ?: return

        // If loginMethod is wechat, or the account is bound
        // to ftc account, do not show the alert.
        if (account.loginMethod != LoginMethod.WECHAT) {
            return
        }

        if (account.isLinked) {
            return
        }

        val wxSession = sessionManager.loadWxSession() ?: return

        if (wxSession.isExpired) {
            logout()
            WxExpireDialogFragment().show(supportFragmentManager, "WxExpireDialog")
        }
    }

    override fun onStart() {
        super.onStart()
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "onStart finished")
        }

        updateSessionUI()
    }

    override fun onBackPressed() {
//        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
//            binding.drawerLayout.closeDrawer(GravityCompat.START)
//        } else {
//            doubleClickToExit()
//        }

        doubleClickToExit()
    }

    private fun doubleClickToExit() {
        // If this is the first time user clicked back button, the first part the will executed.
        if (!mBackKeyPressed) {
            toast(R.string.prompt_exit)
            mBackKeyPressed = true

            // Delay for 2 seconds.
            // If user did not touch the back button within 2 seconds, mBackKeyPressed will be changed back gto false.
            // If user touch the back button within 2 seconds, `if` condition will be false, this part will not be executed.
            launch {
                delay(2000)
                mBackKeyPressed = false
            }
        } else {
            // If user clicked back button two times within 2 seconds, this part will be executed.
            cancel()
            finish()
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


