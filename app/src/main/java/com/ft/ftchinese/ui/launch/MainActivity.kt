package com.ft.ftchinese.ui.launch

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.SearchManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebView
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.SearchView
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.work.*
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.TestActivity
import com.ft.ftchinese.databinding.ActivityMainBinding
import com.ft.ftchinese.databinding.DrawerNavHeaderBinding
import com.ft.ftchinese.model.reader.LoginMethod
import com.ft.ftchinese.model.reader.WX_AVATAR_NAME
import com.ft.ftchinese.model.splash.SplashScreenManager
import com.ft.ftchinese.repository.TabPages
import com.ft.ftchinese.service.AudioDownloadService
import com.ft.ftchinese.service.LatestReleaseWorker
import com.ft.ftchinese.service.VerifySubsWorker
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.store.ServiceAcceptance
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.store.TokenManager
import com.ft.ftchinese.tracking.PaywallTracker
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.ui.about.AboutActivity
import com.ft.ftchinese.ui.account.AccountActivity
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.isActiveNetworkWifi
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.ui.channel.MyftPagerAdapter
import com.ft.ftchinese.ui.channel.SearchableActivity
import com.ft.ftchinese.ui.channel.TabPagerAdapter
import com.ft.ftchinese.ui.login.LoginActivity
import com.ft.ftchinese.ui.login.WxExpireDialogFragment
import com.ft.ftchinese.ui.pay.MemberActivity
import com.ft.ftchinese.ui.paywall.PaywallActivity
import com.ft.ftchinese.ui.settings.SettingsActivity
import com.ft.ftchinese.util.RequestCode
import com.ft.ftchinese.viewmodel.AccountViewModel
import com.ft.ftchinese.viewmodel.Result
import com.ft.ftchinese.viewmodel.SplashViewModel
import com.google.android.exoplayer2.offline.DownloadService
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout
import com.stripe.android.CustomerSession
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast
import org.threeten.bp.LocalDate
import java.io.InputStream

/**
 * MainActivity implements ChannelFragment.OnFragmentInteractionListener to interact with TabLayout.
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class MainActivity : ScopedAppActivity(),
        TabLayout.OnTabSelectedListener,
        AnkoLogger {

    private var bottomDialog: BottomSheetDialog? = null
    private var mBackKeyPressed = false
    private var pagerAdapter: TabPagerAdapter? = null

    private lateinit var accountViewModel: AccountViewModel
    private lateinit var splashViewModel: SplashViewModel
    private lateinit var binding: ActivityMainBinding
    private lateinit var navHeaderBinding: DrawerNavHeaderBinding

    private lateinit var cache: FileCache

    private lateinit var sessionManager: SessionManager
    private lateinit var acceptance: ServiceAcceptance
    private lateinit var tokenManager: TokenManager
    private lateinit var wxApi: IWXAPI
    private lateinit var workManager: WorkManager

    private lateinit var statsTracker: StatsTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        setSupportActionBar(binding.toolbar)

        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        createNotificationChannel()

        cache = FileCache(this)


        statsTracker = StatsTracker.getInstance(this)

        // Register Wechat id
        wxApi = WXAPIFactory.createWXAPI(this, BuildConfig.WX_SUBS_APPID, false)
        wxApi.registerApp(BuildConfig.WX_SUBS_APPID)

        sessionManager = SessionManager.getInstance(this)
        acceptance = ServiceAcceptance.getInstance(this)
        tokenManager = TokenManager.getInstance(this)
        workManager = WorkManager.getInstance(this)

        accountViewModel = ViewModelProvider(this)
                .get(AccountViewModel::class.java)


        // Set ViewPager adapter
        setupHome()

        // Link ViewPager and TabLayout
        binding.tabLayout.setupWithViewPager(binding.viewPager)
        binding.tabLayout.addOnTabSelectedListener(this)

        setupBottomNav()
        setupDrawer()

        // If avatar is downloaded from network.
        accountViewModel.avatarRetrieved.observe(this, {
            onAvatarRetrieved(it)
        })

        if (BuildConfig.DEBUG) {
            info("onCreate finished. Build flavor: ${BuildConfig.FLAVOR}. Is debug: ${BuildConfig.DEBUG}")
        }

        statsTracker.appOpened()

        checkWxSession()

        setupSplashScreen()

        try {
            DownloadService.start(this, AudioDownloadService::class.java)
        } catch (e: Exception) {
            DownloadService.startForeground(this, AudioDownloadService::class.java)
        }

        showTermsAndConditions()
        setupWorker()
    }

    private fun showTermsAndConditions() {
        info("Service accepted ${acceptance.isAccepted()}")
        // Service acceptance
        if (!acceptance.isAccepted()) {
            val frag = AcceptServiceDialogFragment()
            supportFragmentManager.commit {
                add(android.R.id.content, frag)
                addToBackStack(null)
            }
        }
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
            info("verifyWork state ${workInfo.state}")
        }

        val upgradeWork = OneTimeWorkRequestBuilder<LatestReleaseWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork("latestRelease", ExistingWorkPolicy.REPLACE, upgradeWork)
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnNavigationItemSelectedListener {
            info("Selected bottom nav item ${it.title}")

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

        supportActionBar?.setDisplayUseLogoEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setLogo(R.drawable.ic_menu_masthead)
    }

    /**
     * Add event listener to drawer menu.
     */
    private fun setupDrawer() {

        navHeaderBinding = DataBindingUtil.inflate(
                layoutInflater,
                R.layout.drawer_nav_header,
                binding.drawerNav,
                false)

        binding.drawerNav.addHeaderView(navHeaderBinding.root)

        // For testing
        binding.drawerNav.menu
            .setGroupVisible(R.id.drawer_group3, BuildConfig.DEBUG)

        val toggle = ActionBarDrawerToggle(
                this,
                binding.drawerLayout,
                binding.toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close)
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Set a listener that will be notified when a menu item is selected.
        binding.drawerNav.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.action_login ->  LoginActivity.startForResult(this)
                R.id.action_account -> AccountActivity.start(this)
                R.id.action_paywall -> {
                    // Tracking
                    PaywallTracker.fromDrawer()

                    PaywallActivity.start(this)
                }
                R.id.action_my_subs -> MemberActivity.start(this)
                R.id.action_feedback -> feedbackEmail()
                R.id.action_settings -> SettingsActivity.start(this)
                R.id.action_about -> AboutActivity.start(this)
                R.id.action_test -> TestActivity.start(this)
            }

            binding.drawerLayout.closeDrawer(GravityCompat.START)

            true
        }

        // Set listener on the title text inside drawer's header view
        navHeaderBinding.navHeaderTitle.setOnClickListener {
            if (!sessionManager.isLoggedIn()) {
                LoginActivity.startForResult(this)
                return@setOnClickListener
            }

            // Setup bottom dialog
            if (bottomDialog == null) {
                bottomDialog = BottomSheetDialog(this)
                bottomDialog?.setContentView(R.layout.fragment_logout)
            }

            bottomDialog?.findViewById<TextView>(R.id.action_logout)?.setOnClickListener{
                logout()

                bottomDialog?.dismiss()
                toast("账号已登出")
            }

            bottomDialog?.show()
        }

        updateSessionUI()
    }

    /**
     * Update UI depending on user's login/logout state
     */
    private fun updateSessionUI() {

        val account = sessionManager.loadAccount()

        navHeaderBinding.account = account
        // show signin/signup if account is null.
        binding.drawerNav.menu.setGroupVisible(R.id.drawer_group_sign_in_up, account == null)

        // Show login/signup if account does not exist.
        binding.drawerNav.menu
                .findItem(R.id.action_account)
                ?.isVisible = account != null

        // Only show when user is a member.
        binding.drawerNav.menu
                .findItem(R.id.action_my_subs)
                ?.isVisible = account?.isMember ?: false

        // If account is null, or not a member, show paywall.
        binding.drawerNav.menu
                .findItem(R.id.action_paywall)
                ?.isVisible = !(account?.isMember ?: false)

        // Load wechat avatar.
        if (account != null && isConnected) {
            accountViewModel.fetchWxAvatar(
                    cache,
                    account.wechat
            )
        }
    }

    private fun onAvatarRetrieved(result: Result<InputStream>) {
        when (result) {
            is Result.LocalizedError -> {
                info(getString(result.msgId))
            }
            is Result.Error -> {
                info(result.exception)
            }
            is Result.Success -> {

                navHeaderBinding.avatar = Drawable.createFromStream(
                        result.data,
                        WX_AVATAR_NAME
                )
            }
        }
    }

    private fun logout() {
        sessionManager.logout()
        cache.deleteFile(WX_AVATAR_NAME)
        CustomerSession.endCustomerSession()
        updateSessionUI()
    }

    private fun displayTitle(title: Int) {
        supportActionBar?.setDisplayUseLogoEnabled(false)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.setTitle(title)
    }

    private fun setupSplashScreen() {

        splashViewModel = ViewModelProvider(this)
                .get(SplashViewModel::class.java)

        splashViewModel.screenAdSelected.observe(this, {
            SplashScreenManager(this).save(it, LocalDate.now())
        })


        splashViewModel.prepareNextRound(
                cache,
                isActiveNetworkWifi(),
                sessionManager.loadAccount()?.membership?.tier
        )
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

        // For testing only. To test wechat you must use a release version.
//        WxExpireDialogFragment().show(supportFragmentManager, "WxExpireDialog")

        if (wxSession.isExpired) {
            logout()
            WxExpireDialogFragment().show(supportFragmentManager, "WxExpireDialog")
        }
    }

    override fun onStart() {
        super.onStart()
        if (BuildConfig.DEBUG) {
            info("onStart finished")
        }

        updateSessionUI()
    }

    /**
     * Deal with the cases that an activity launched by this activity exits.
     * For example, the LoginActvity will automatically finish when it successfully logged in,
     * and then it should inform the MainActivity to update UI for a logged in mUser.
     * `requestCode` is used to identify who this result cam from. We are using it to identify if the result came from LoginActivity or SignupActivity.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (BuildConfig.DEBUG) {
            info("onActivityResult: requestCode $requestCode, resultCode $resultCode")
        }

        when (requestCode) {
            // If the result come from SignIn or SignUp, update UI to show mUser login state.
            RequestCode.SIGN_IN, RequestCode.SIGN_UP -> {

                if (resultCode != Activity.RESULT_OK) {
                    return
                }

                toast(R.string.prompt_logged_in)
                updateSessionUI()
            }
        }
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            doubleClickToExit()
        }
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
        // Inflate the menu; this adds mFollows to the action bar if it is present.

        menuInflater.inflate(R.menu.activity_main_search, menu)


        // Get the SearchView and set the searchable configuration
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager

        val searchView = (menu.findItem(R.id.action_search).actionView as SearchView)

        // NOTE: If you followed example verbatim from
        // https://developer.android.com/guide/topics/search/search-dialog.html#UsingSearchWidget,
        // it won't work!
        // The `componentName` passed to getSearchableInfo
        // should be the target activity used to display
        // search result.
        // If you simply use `componentName`, it refers
        // the the MainActivity here, which is not used
        // to display search result.
        val compoName = ComponentName(this, SearchableActivity::class.java)

        searchView.apply {
            // Assumes current activity is the searchable activity
            setSearchableInfo(searchManager.getSearchableInfo(compoName))
            setIconifiedByDefault(false)
        }

        return true
    }

    /**
     * Respond to menu item on the toolbar being selected
     */
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
            R.id.action_search -> {
                super.onOptionsItemSelected(item)
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
            info("Tab selected: ${tab?.position}")
        }

        val position = tab?.position ?: return
        val title = pagerAdapter?.getPageTitle(position) ?: return

        if (BuildConfig.DEBUG) {
            info("View item list event: $title")
        }

        statsTracker.tabSelected(title.toString())
    }

    override fun onTabReselected(tab: TabLayout.Tab?) {
        if (BuildConfig.DEBUG) {
            info("Tab reselected: ${tab?.position}")
        }
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) {
        if (BuildConfig.DEBUG) {
            info("Tab unselected: ${tab?.position}")
        }
    }

    private fun feedbackEmail() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:ftchinese.feedback@gmail.com")
            putExtra(Intent.EXTRA_SUBJECT, "Feedback on FTC Android App")
        }

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            toast(R.string.prompt_no_email_app)
        }
    }

    companion object {
        @JvmStatic
        fun start(context: Context) {
            context.startActivity(Intent(context, MainActivity::class.java))
        }
    }
}


