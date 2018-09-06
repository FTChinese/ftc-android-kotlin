package com.ft.ftchinese

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.support.design.widget.*
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.SearchView
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import com.ft.ftchinese.models.*
import com.ft.ftchinese.user.*
import com.ft.ftchinese.util.Store
import com.ft.ftchinese.util.gson
import com.koushikdutta.ion.Ion
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.android.UI
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast
import org.joda.time.LocalDate
import org.joda.time.format.ISODateTimeFormat
import java.nio.file.FileSystem

const val REQUEST_CODE_SIGN_IN = 1
const val REQUEST_CODE_SIGN_UP = 2

class MainActivity : AppCompatActivity(),
        NavigationView.OnNavigationItemSelectedListener,
        TabLayout.OnTabSelectedListener,
        SectionFragment.OnFragmentInteractionListener,
        ChannelWebViewClient.OnInAppNavigate,
        AnkoLogger {

    private var mBottomDialog: BottomSheetDialog? = null
    private var user: User? = null
    private var mBackKeyPressed = false
    private var exitJob: Job? = null
    private var timerJob: Job? = null

    /**
     * Implementation of BottomNavigationView.OnNavigationItemSelectedListener
     */
    private val bottomNavItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
       info("Selected bottom nav item ${item.title}")

        when (item.itemId) {
            R.id.nav_news -> {
                supportActionBar?.setTitle(R.string.app_name)
                view_pager.adapter = SectionsPagerAdapter(ListPage.newsPages, supportFragmentManager)

            }

            R.id.nav_english -> {
                supportActionBar?.setTitle(R.string.nav_english)
                view_pager.adapter = SectionsPagerAdapter(ListPage.englishPages, supportFragmentManager)

            }

            R.id.nav_ftacademy -> {
                supportActionBar?.setTitle(R.string.nav_ftacademy)
                view_pager.adapter = SectionsPagerAdapter(ListPage.ftaPages, supportFragmentManager)

            }

            R.id.nav_video -> {
                supportActionBar?.setTitle(R.string.nav_video)
                view_pager.adapter = SectionsPagerAdapter(ListPage.videoPages, supportFragmentManager)

            }

            R.id.nav_myft -> {
                view_pager.adapter = MyftPagerAdapter(MyftTab.pages, supportFragmentManager)
            }
        }
        true
    }

    private val logoutListener = View.OnClickListener {
        User.removeFromPref(this)
        updateUIForCookie()
        mBottomDialog?.dismiss()
        toast("账号已登出")
    }

    private val drawerHeaderTitleListener = View.OnClickListener {
        // If user is not logged in, show login.
        if (user == null) {
            LoginActivity.startForResult(this, REQUEST_CODE_SIGN_IN)
            return@OnClickListener
        }

        // If user already logged in, show logout.
        if (mBottomDialog == null) {
            mBottomDialog = BottomSheetDialog(this)
            mBottomDialog?.setContentView(R.layout.fragment_logout)
        }

        mBottomDialog?.findViewById<TextView>(R.id.action_logout)?.setOnClickListener(logoutListener)

        mBottomDialog?.show()
    }

    /**
     * Implementation of OnNavigationItemReselectedListener.
     * Currently do nothing.
     */
    private val bottomNavItemReseletedListener = BottomNavigationView.OnNavigationItemReselectedListener { item ->
        info("Reselected bottom nav item: ${item.title}")
    }

    lateinit var api: IWXAPI

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // https://developer.android.com/training/system-ui/immersive
//        hideSystemUI()
        showAd()

//        setTheme(R.style.Origami)

        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        // Set ViewPager adapter
        view_pager.adapter = SectionsPagerAdapter(ListPage.newsPages, supportFragmentManager)

        // Link ViewPager and TabLayout
        tab_layout.setupWithViewPager(view_pager)
        tab_layout.addOnTabSelectedListener(this)

        // Bottom navigation listener
        bottom_nav.setOnNavigationItemSelectedListener(bottomNavItemSelectedListener)
        bottom_nav.setOnNavigationItemReselectedListener(bottomNavItemReseletedListener)

        // Check user login status
        updateUIForCookie()

        // Set a listener that will be notified when a menu item is selected.
        drawer_nav.setNavigationItemSelectedListener(this)

        // Set listener on the title text inside drawer's header view
        drawer_nav.getHeaderView(0)
                ?.findViewById<TextView>(R.id.nav_header_title)
                ?.setOnClickListener(drawerHeaderTitleListener)

        api = WXAPIFactory.createWXAPI(this, BuildConfig.WECAHT_APP_ID, false)
        api.registerApp(BuildConfig.WECAHT_APP_ID)


        checkAd()
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LOW_PROFILE or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }

    private fun showSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    private fun showAd() {
        val todaySchedules = LaunchSchedule.loadFromPref(this)

        val ad = todaySchedules[0]

        if (!Store.exists(this, ad.imageName)) {
            return
        }

        // Read this article on how inflate works:
        // https://www.bignerdranch.com/blog/understanding-androids-layoutinflater-inflate/
        val adView = View.inflate(this, R.layout.ad_view, null)
        root_container.addView(adView)
        val adImage = adView.findViewById<ImageView>(R.id.ad_image)
        val adTimer = adView.findViewById<TextView>(R.id.ad_timer)

        adTimer.setOnClickListener {
            toast("Clicked timer")
            root_container.removeView(adView)
//            showSystemUI()
        }

        adImage.setOnClickListener {
            val customTabsInt = CustomTabsIntent.Builder().build()
            customTabsInt.launchUrl(this, Uri.parse(ad.linkUrl))
            root_container.removeView(adView)
//            showSystemUI()
            timerJob?.cancel()
            info("Clicked ads")
        }

        timerJob = launch(UI) {
            val result = async {
                Drawable.createFromStream(openFileInput(ad.imageName), ad.imageName)
            }

            val drawable = result.await()

            adImage.setImageDrawable(drawable)

            for (i in 5 downTo 1) {
                adTimer.text = getString(R.string.prompt_ad_timer, i)
                delay(1000)
            }

            root_container.removeView(adView)
//            showSystemUI()
        }
    }

    private fun checkAd() {
        launch(CommonPool) {
            val schedules = LaunchSchedule.fetchData()
            schedules?.save(this@MainActivity)

            downloadAdImage()
        }
    }

    private fun downloadAdImage() {
        val adsToDownload = LaunchSchedule.loadFromPref(this, days = 1)

        for (ad in adsToDownload) {
            ad.cacheImage(this)
        }
    }

    override fun onRestart() {
        super.onRestart()
        info("onRestart finished")
    }

    override fun onStart() {
        super.onStart()
        info("onStart finished")
        updateUIForCookie()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {

        super.onRestoreInstanceState(savedInstanceState)

        info("onRestoreInstanceSate finished")
    }

    /**
     * Deal with the cases that an activity launched by this activity exits.
     * For example, the LoginActvity will automatically finish when it successfully logged in,
     * and then it should inform the MainActivity to update UI for a logged in user.
     * `requestCode` is used to identify who this result cam from. We are using it to identify if the result came from LoginActivity or SignupActivity.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        info("onActivityResult: requestCode $requestCode, resultCode $resultCode")

        when (requestCode) {
            // If the result come from SignIn or SignUp, update UI to show user login state.
            REQUEST_CODE_SIGN_IN, REQUEST_CODE_SIGN_UP -> {
                if (resultCode == Activity.RESULT_OK) {
                    toast("登录成功")
                    updateUIForCookie()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        info("onResume finished")
    }

    override fun onPause() {
        super.onPause()
        info("onPause finished")
    }

    override fun onStop() {
        super.onStop()
        info("onStop finished")
    }

    override fun onDestroy() {
        super.onDestroy()

        timerJob?.cancel()

        info("onDestroy finished")
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)

        info("onSaveInstanceSate finished")
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            doubleClickToExit()
        }
    }

    private fun doubleClickToExit() {
        if (!mBackKeyPressed) {
            toast("再按一次退出程序")
            mBackKeyPressed = true

            exitJob = launch(CommonPool) {
                delay(2000)
                mBackKeyPressed = false
            }
        } else {
            exitJob?.cancel()
            finish()
        }
    }

    /**
     * Create menus on toolbar
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds mFollows to the action bar if it is present.

        menuInflater.inflate(R.menu.activity_main_search, menu)

        val expandListener = object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                info("Menu item action collapse")
                return true
            }

            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                info("Menu item action expand")
                return true
            }
        }

        // Configure action view.
        // See https://developer.android.com/training/appbar/action-views
        val searchItem = menu.findItem(R.id.action_search)
        searchItem.setOnActionExpandListener(expandListener)

        val searchView = searchItem.actionView as SearchView

        // Handle activity_main_search. See
        // guide https://developer.android.com/guide/topics/search/
        // API https://developer.android.com/reference/android/support/v7/widget/SearchView

        return super.onCreateOptionsMenu(menu)
    }

    /**
     * Respond to menu item on the toolbar being selected
     */
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
            R.id.action_search -> {
                info("Clicked activity_main_search")
                super.onOptionsItemSelected(item)
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }

    /**
     * Listener for drawer menu selection
     * Implements NavigationView.OnNavigationItemSelectedListener
     */
    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.action_login -> {
                LoginActivity.startForResult(this, REQUEST_CODE_SIGN_IN)
            }
            R.id.action_sign_up -> {
                Registration.startForResult(this, REQUEST_CODE_SIGN_UP)
            }
            R.id.action_account -> {
                AccountActivity.start(this)
            }
            R.id.action_subscription -> {
                MembershipActivity.start(this)
            }
//            R.id.action_help -> {
//
//            }
            R.id.action_about -> {
                AboutUsActivity.start(this)
            }
            R.id.action_feedback -> {
                feedbackEmail()
            }
            R.id.action_settings -> {
                SettingsActivity.start(this)
            }
            R.id.action_logout -> {
                // Delete user data from shared preference and update UI.
                User.removeFromPref(this)
                updateUIForCookie()
                toast("账号已登出")
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    /**
     * Implementation of TabLayout.OnTabSelectedListener
     */
    override fun onTabSelected(tab: TabLayout.Tab?) {
        info("Tab position: ${tab?.position}")
        info("Tab selected: ${tab_layout.selectedTabPosition}")
    }

    override fun onTabReselected(tab: TabLayout.Tab?) {
        info("Tab reselected: ${tab?.position}")
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) {
        info("Tab unselected: ${tab?.position}")
    }

    /**
     * Implementation of SectionFragment.OnDataLoadListener.
     * This is used to handle the state of progress bar.
     */
    override fun onProgress(show: Boolean) {
        if (show) {
            progress_bar.visibility = View.VISIBLE
        } else {
            progress_bar.visibility = View.GONE
        }
    }

    /**
     * Implementation of ChannelWebViewClient.OnInAppNavigate
     * Use cases:
     * When user clicked links on the frontpage like `每日英语`,
     * the app should actually jump to the second item in bottom navigation instead of opening a separate activity.
     */
    override fun selectBottomNavItem(itemId: Int) {
        val item = bottom_nav.menu.findItem(itemId)

        if (item != null) {
            item.isChecked = true
            // You should also call this method to make view visible.
            // Set `isChecked` only changes the menu item's own state
            bottomNavItemSelectedListener.onNavigationItemSelected(item)
        }

    }

    override fun selectTabLayoutTab(tabIndex: Int) {
        tab_layout.getTabAt(tabIndex)?.select()
    }

    /**
     * Update UI dpending user's login/logout state
     */
    private fun updateUIForCookie() {
        user = User.loadFromPref(this)
        val isLoggedIn = (user != null)

        val menu = drawer_nav.menu
        val header = drawer_nav.getHeaderView(0)

        // If seems this is the only way to get the header view.
        // You cannot user `import kotlinx.android.synthetic.activity_main_search.drawer_nav_header.*`,
        // which will give you null pointer exception.
        header.findViewById<TextView>(R.id.nav_header_title).text = if (!user?.name.isNullOrBlank()) {
            user?.name
        } else if (!user?.email.isNullOrBlank()) {
            user?.email
        } else {
            getString(R.string.nav_not_logged_in)
        }

        menu.setGroupVisible(R.id.drawer_group_sign_in_up, !isLoggedIn)
        menu.findItem(R.id.action_account).isVisible = isLoggedIn
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
            toast("您的设备上没有安装邮件程序，无法发送反馈邮件")
        }
    }

    companion object {
        fun start(context: Context?) {
            val intent = Intent(context, MainActivity::class.java)
            context?.startActivity(intent)
        }
    }

    /**
     * A [FragmentPagerAdapter] that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    inner class SectionsPagerAdapter(private val pages: Array<ListPage>, fm: FragmentManager) : FragmentStatePagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            // getItem is called to instantiate the fragment for the given page.
            info("Fragment data: ${pages[position]}")
            return SectionFragment.newInstance(pages[position])
        }

        override fun getCount(): Int {
            return pages.size
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return pages[position].title
        }
    }

    inner class MyftPagerAdapter(private val pages: Array<MyftTab>, fm: FragmentManager) : FragmentStatePagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            val page = pages[position]
            return if (page.id == MyftTab.FOLLOWING) {
                FollowingFragment.newInstance()
            } else {
                MyftFragment.newInstance(page.id)
            }
        }

        override fun getCount(): Int {
            return pages.size
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return pages[position].title
        }
    }
}


