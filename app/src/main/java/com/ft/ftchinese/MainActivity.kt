package com.ft.ftchinese

import android.app.Activity
import android.app.SearchManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.view.GravityCompat
import androidx.appcompat.app.ActionBarDrawerToggle
import android.view.*
import android.webkit.WebView
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.ft.ftchinese.base.ScopedAppActivity
import com.ft.ftchinese.base.isActiveNetworkWifi
import com.ft.ftchinese.base.isNetworkConnected
import com.ft.ftchinese.model.*
import com.ft.ftchinese.splash.SplashScreenManager
import com.ft.ftchinese.ui.account.AccountActivity
import com.ft.ftchinese.ui.account.AccountViewModel
import com.ft.ftchinese.ui.login.LoginActivity
import com.ft.ftchinese.ui.login.WxExpireDialogFragment
import com.ft.ftchinese.ui.account.MemberActivity
import com.ft.ftchinese.ui.channel.MyftPagerAdapter
import com.ft.ftchinese.ui.channel.SearchableActivity
import com.ft.ftchinese.ui.channel.TabPagerAdapter
import com.ft.ftchinese.ui.pay.PaywallActivity
import com.ft.ftchinese.ui.settings.SettingsActivity
import com.ft.ftchinese.ui.splash.SplashViewModel
import com.ft.ftchinese.util.*
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.drawer_nav_header.view.*
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast
import java.io.ByteArrayInputStream

/**
 * MainActivity implements ChannelFragment.OnFragmentInteractionListener to interact with TabLayout.
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class MainActivity : ScopedAppActivity(),
        TabLayout.OnTabSelectedListener,
        AnkoLogger {

    private var bottomDialog: BottomSheetDialog? = null
    private var mBackKeyPressed = false

    private var avatarName: String? = null

    private var showAdJob: Job? = null

    private lateinit var accountViewModel: AccountViewModel
    private lateinit var splashViewModel: SplashViewModel

//    private var mNewsAdapter: TabPagerAdapter? = null
//    private var mEnglishAdapter: TabPagerAdapter? = null
//    private var mFtaAdapter: TabPagerAdapter? = null
//    private var mVideoAdapter: TabPagerAdapter? = null
//    private var mMyftPagerAdapter: MyftPagerAdapter? = null

    private var mChannelPages: Array<ChannelSource>? = null

    private lateinit var cache: FileCache

    private lateinit var sessionManager: SessionManager
    private lateinit var tokenManager: TokenManager
    private lateinit var splashManager: SplashScreenManager
    private lateinit var wxApi: IWXAPI

    private lateinit var statsTracker: StatsTracker

    // Cache UI
//    private var drawerHeaderTitle: TextView? = null
//    private var drawerHeaderImage: ImageView? = null
    private var headerView: View? = null
    private var menuItemAccount: MenuItem? = null
    private var menuItemSubs: MenuItem? = null
    private var menuItemMySubs: MenuItem? = null

    /**
     * Update UI depending on user's login/logout state
     */
    private fun updateSessionUI() {
        val account = sessionManager.loadAccount()

        if (account == null) {
            headerView?.nav_header_title?.text = getString(R.string.nav_not_logged_in)
            headerView?.nav_header_image?.setImageResource(R.drawable.ic_account_circle_black_24dp)

            // show signin/signup
            drawer_nav.menu
                    .setGroupVisible(R.id.drawer_group_sign_in_up, true)

            // Do not show account
            menuItemAccount?.isVisible = false
            // Show subscription
            menuItemSubs?.isVisible = true
            // Do not show my subscription
            menuItemMySubs?.isVisible = false
            return
        }

        headerView?.nav_header_title?.text = account.displayName
        showAvatar(account.wechat)

        // Hide signin/signup
        drawer_nav.menu
                .setGroupVisible(R.id.drawer_group_sign_in_up, false)
        // Show account
        menuItemAccount?.isVisible = true

        // If user is not logged in, isMember always false.
        val isMember = account.isMember

        // Show subscription if user is a member; otherwise
        // hide it
        menuItemSubs?.isVisible = !isMember
        // Show my subscription if user is a member; otherwise hide it.
        menuItemMySubs?.isVisible = isMember
    }

    private fun showAvatar(wechat: Wechat) {

        launch {
            val drawable = withContext(Dispatchers.IO) {
                cache.readDrawable(WX_AVATAR_NAME)
            }

            if (drawable != null) {
                headerView?.nav_header_image?.setImageDrawable(drawable)
                return@launch
            }

            if (!isNetworkConnected()) {
                return@launch
            }

            if (wechat.avatarUrl == null) {
                return@launch
            }

            accountViewModel.fetchWxAvatar(cache, wechat)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setTheme(R.style.Origami)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        displayLogo(true)

        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        cache = FileCache(this)
        splashManager = SplashScreenManager(this)

        // Show advertisement
        // Keep a reference the coroutine in case user exit at this moment
        showAdJob = launch {
            showAd()
        }

        statsTracker = StatsTracker.getInstance(this)

        // Register Wechat id
        wxApi = WXAPIFactory.createWXAPI(this, BuildConfig.WX_SUBS_APPID, false)
        wxApi.registerApp(BuildConfig.WX_SUBS_APPID)

        sessionManager = SessionManager.getInstance(this)
        tokenManager = TokenManager.getInstance(this)

        val menu = drawer_nav.menu
        headerView = drawer_nav.getHeaderView(0)

        menuItemAccount = menu.findItem(R.id.action_account)
        menuItemSubs = menu.findItem(R.id.action_subscription)
        menuItemMySubs = menu.findItem(R.id.action_my_subs)

        accountViewModel = ViewModelProviders.of(this)
                .get(AccountViewModel::class.java)

        splashViewModel = ViewModelProviders.of(this)
                .get(SplashViewModel::class.java)

        // If avatar is downloaded from network.
        accountViewModel.avatarResult.observe(this, Observer {
            if (it.exception != null) {
                info("Loading avatar error: ${it.exception}")
                return@Observer
            }

            val bytes = it.success ?: return@Observer

            headerView?.nav_header_image?.setImageDrawable(
                    Drawable.createFromStream(
                            ByteArrayInputStream(bytes),
                            WX_AVATAR_NAME
                    )
            )
        })

        splashViewModel.scheduleResult.observe(this, Observer {
            splashViewModel.prepare(
                    sessionManager = sessionManager,
                    splashManager = splashManager,
                    schedule = it
            )
        })

        // Set ViewPager adapter
        setupHome()

        // Link ViewPager and TabLayout
        tab_layout.setupWithViewPager(view_pager)
        tab_layout.addOnTabSelectedListener(this)

        setupBottomNav()

        setupDrawer()

        // Update UI.
        updateSessionUI()

        splashViewModel.loadSchedule(cache, isActiveNetworkWifi())

        if (BuildConfig.DEBUG) {
            info("onCreate finished. Build flavor: ${BuildConfig.FLAVOR}. Is debug: ${BuildConfig.DEBUG}")
        }

        statsTracker.appOpened()
    }

    private fun setupBottomNav() {
        bottom_nav.setOnNavigationItemSelectedListener {
            info("Selected bottom nav item ${it.title}")

            when (it.itemId) {
                R.id.nav_news -> {
                    setupHome()

                    displayLogo(true)
                }

                R.id.nav_english -> {
//                    if (mEnglishAdapter == null) {
//                        mEnglishAdapter = TabPagerAdapter(Navigation.englishPages, supportFragmentManager)
//                    }
                    view_pager.adapter = TabPagerAdapter(Navigation.englishPages, supportFragmentManager)
                    mChannelPages = Navigation.englishPages

                    displayTitle(R.string.nav_english)
                }

                R.id.nav_ftacademy -> {
//                    if (mFtaAdapter == null) {
//                        mFtaAdapter = TabPagerAdapter(Navigation.ftaPages, supportFragmentManager)
//                    }
                    view_pager.adapter = TabPagerAdapter(Navigation.ftaPages, supportFragmentManager)
                    mChannelPages = Navigation.ftaPages

                    displayTitle(R.string.nav_ftacademy)
                }

                R.id.nav_video -> {
//                    if (mVideoAdapter == null) {
//                        mVideoAdapter = TabPagerAdapter(Navigation.videoPages, supportFragmentManager)
//                    }
                    view_pager.adapter = TabPagerAdapter(Navigation.videoPages, supportFragmentManager)
                    mChannelPages = Navigation.videoPages

                    displayTitle(R.string.nav_video)
                }

                R.id.nav_myft -> {
//                    if (mMyftPagerAdapter == null) {
//                        mMyftPagerAdapter = MyftPagerAdapter(supportFragmentManager)
//                    }
                    view_pager.adapter = MyftPagerAdapter(supportFragmentManager)
                    mChannelPages = null

                    displayTitle(R.string.nav_myft)
                }
            }
            true
        }
    }

    /**
     * Add event listener to drawer menu.
     */
    private fun setupDrawer() {
        val toggle = ActionBarDrawerToggle(
                this,
                drawer_layout,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        // Set a listener that will be notified when a menu item is selected.
        drawer_nav.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.action_login ->  LoginActivity.startForResult(this)
                R.id.action_account -> AccountActivity.start(this)
                R.id.action_subscription -> {
                    // Tracking
                    PaywallTracker.fromDrawer()

                    PaywallActivity.start(this)
                }
                R.id.action_my_subs -> MemberActivity.start(this)
                R.id.action_feedback -> feedbackEmail()
                R.id.action_settings -> SettingsActivity.start(this)
                R.id.action_app_download -> {
                    val webpage: Uri = Uri.parse("http://app.ftchinese.com/androidmobile.html")
                    val intent = Intent(Intent.ACTION_VIEW, webpage)
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                    }
                }
            }

            drawer_layout.closeDrawer(GravityCompat.START)

            true
        }

        // Set listener on the title text inside drawer's header view
        drawer_nav.getHeaderView(0)
                ?.findViewById<TextView>(R.id.nav_header_title)
                ?.setOnClickListener {
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
    }

    private fun logout() {
        sessionManager.logout()
        cache.deleteFile(avatarName)
        updateSessionUI()
    }

    private fun setupHome() {
//        if (mNewsAdapter == null) {
//            mNewsAdapter = TabPagerAdapter(Navigation.newsPages, supportFragmentManager)
//        }
        view_pager.adapter = TabPagerAdapter(Navigation.newsPages, supportFragmentManager)
        mChannelPages = Navigation.newsPages
    }

    private fun displayLogo(show: Boolean) {
        if (show) {
            supportActionBar?.setDisplayUseLogoEnabled(true)
            supportActionBar?.setDisplayShowTitleEnabled(false)
            supportActionBar?.setLogo(R.drawable.ic_menu_masthead)
        } else {
            supportActionBar?.setDisplayUseLogoEnabled(false)
            supportActionBar?.setDisplayShowTitleEnabled(false)
        }
    }

    private fun displayTitle(title: Int) {
        supportActionBar?.setDisplayUseLogoEnabled(false)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.setTitle(title)
    }

    private fun showSystemUI() {
        supportActionBar?.show()
        root_container.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN


        checkWxSession()
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


    private suspend fun showAd() {
        val screenAd = splashManager.load() ?: return

        // Pick an ad based on its probability distribution factor.
        GlobalScope.launch {
            val drawable = withContext(Dispatchers.IO) {
                if (!screenAd.isToday()) {
                    return@withContext null
                }

                if (BuildConfig.DEBUG) {
                    info("Splash screen ad: $screenAd")
                }

                val imageFileName = screenAd.imageName

                // Check if the required ad image exists.
                if (imageFileName.isBlank() || !cache.exists(imageFileName)) {
                    if (BuildConfig.DEBUG) {
                        info("Ad image ${screenAd.imageName} not found")
                    }

                    return@withContext null
                }

                cache.readDrawable(imageFileName)
            }

            if (drawable == null) {
                if (BuildConfig.DEBUG) {
                    info("Cannot load ad image")
                }

                showSystemUI()
                return@launch
            }

            withContext(Dispatchers.Main) {
                val adView = createAdView()
                val adImage = adView.findViewById<ImageView>(R.id.ad_image)
                val adTimer = adView.findViewById<TextView>(R.id.ad_timer)

                adTimer.setOnClickListener {
                    root_container.removeView(adView)
                    showSystemUI()
                    showAdJob?.cancel()
                    if (BuildConfig.DEBUG) {
                        info("Skipped ads")
                    }

                    // Log user skipping advertisement action.
                    statsTracker.adSkipped(screenAd)
                }


                adImage.setOnClickListener {
                    val customTabsInt = CustomTabsIntent.Builder().build()
                    customTabsInt.launchUrl(this@MainActivity, Uri.parse(screenAd.linkUrl))
                    root_container.removeView(adView)
                    showSystemUI()
                    showAdJob?.cancel()

                    // Log click event
                    statsTracker.adClicked(screenAd)

                    if (BuildConfig.DEBUG) {
                        info("Clicked ads")
                    }
                }


                adImage.setImageDrawable(drawable)

                adTimer.visibility = View.VISIBLE
                info("Show timer")

                // send impressions in background.
                splashViewModel.sendImpression(screenAd, statsTracker)

                // Tracking ad viewd
                statsTracker.adViewed(screenAd)

                for (i in 5 downTo 1) {
                    adTimer.text = getString(R.string.prompt_ad_timer, i)
                    delay(1000)
                }

                root_container.removeView(adView)

                showSystemUI()
            }
        }
    }

    // Read this article on how inflate works:
    // https://www.bignerdranch.com/blog/understanding-androids-layoutinflater-inflate/
    private fun createAdView(): View {
        // Read this article on how inflate works:
        // https://www.bignerdranch.com/blog/understanding-androids-layoutinflater-inflate/
        val adView = View.inflate(this, R.layout.ad_view, null)

        if (BuildConfig.DEBUG) {
            info("Starting to show ad. Hide system ui.")
        }

        supportActionBar?.hide()
        adView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LOW_PROFILE or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

        if (BuildConfig.DEBUG) {
            info("Added ad view")
        }

        root_container.addView(adView)

        return adView
    }

    override fun onRestart() {
        super.onRestart()
        if (BuildConfig.DEBUG) {
            info("onRestart finished")
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

    override fun onResume() {
        super.onResume()

        checkDeviceToken()

        if (BuildConfig.DEBUG) {
            info("onResume finished")
        }
    }

    private fun checkDeviceToken() {
        if (BuildConfig.DEBUG) {
            val token = tokenManager.getToken()
            info("Device token $token")
        }
    }

    override fun onPause() {
        super.onPause()
        if (BuildConfig.DEBUG) {
            info("onPause finished")
        }

        showAdJob?.cancel()
    }

    override fun onStop() {
        super.onStop()
        if (BuildConfig.DEBUG) {
            info("onStop finished")
        }

        showAdJob?.cancel()
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
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
        val pages = mChannelPages ?: return

        if (BuildConfig.DEBUG) {
            info("View item list event: ${pages[position]}")
        }

        statsTracker.tabSelected(pages[position].title)
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
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, "ftchinese.feedback@gmail.com")
            putExtra(Intent.EXTRA_SUBJECT, "Feedback on FTC Android App")
        }

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            toast(R.string.prompt_no_email_app)
        }
    }
}


