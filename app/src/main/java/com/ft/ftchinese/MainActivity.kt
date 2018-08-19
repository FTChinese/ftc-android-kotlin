package com.ft.ftchinese

import android.app.Activity
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.design.widget.NavigationView
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.*
import android.support.v7.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import com.ft.ftchinese.database.ArticleBaseHelper
import com.ft.ftchinese.database.ReadingHistory
import com.ft.ftchinese.models.ChannelItem
import com.ft.ftchinese.models.ListPage
import com.ft.ftchinese.models.MyftTab
import com.ft.ftchinese.models.User
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info


class MainActivity : AppCompatActivity(),
        NavigationView.OnNavigationItemSelectedListener,
        TabLayout.OnTabSelectedListener,
        SectionFragment.OnFragmentInteractionListener,
        ChannelWebViewClient.OnInAppNavigate,
        AnkoLogger {

    private var dbHelper: ArticleBaseHelper? = null
    private var readingHistory: ReadingHistory? = null
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

    /**
     * Implementation of OnNavigationItemReselectedListener.
     * Currently do nothing.
     */
    private val bottomNavItemReseletedListener = BottomNavigationView.OnNavigationItemReselectedListener { item ->
        info("Reselected bottom nav item: ${item.title}")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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

        // Set a listener that will be notified when a menu item is selected.
        drawer_nav.setNavigationItemSelectedListener(this)

        dbHelper = ArticleBaseHelper(this)
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
     * Deal with the cases that an activity lunched by this activity exits.
     * For example, the LoginActvity will automatically finish when it successfully logged in,
     * and then it should inform the MainActivity to update UI for a logged in user.
     * `requestCode` is used to identify who this result cam from.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show()

        info("onActivityResult: requestCode $requestCode, resultCode $resultCode")

        when (resultCode) {
            Activity.RESULT_OK -> {
                updateUIForCookie()
            }
        }
    }

    /**
     * Update UI dpending user's login/logout state
     */
    private fun updateUIForCookie() {
        val user = User.loadFromPref(this)

        val menu = drawer_nav.menu
        val header = drawer_nav.getHeaderView(0)

        if (user == null) {

            // If seems this is the only way to get the header view.
            // You cannot user `import kotlinx.android.synthetic.search.nav_header_main.*`,
            // which will give you null pointer exception.
            header.findViewById<TextView>(R.id.nav_header_subtitle).setText(R.string.nav_header_subtitle)


            // If user is logged in, how login menu and hide logout menu
            menu.setGroupVisible(R.id.drawer_group0, true)
            menu.setGroupVisible(R.id.drawer_group3, false)
            return
        }

        header.findViewById<TextView>(R.id.nav_header_subtitle).text = user.email

        menu.setGroupVisible(R.id.drawer_group0, false)
        menu.setGroupVisible(R.id.drawer_group3, true)
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
            super.onBackPressed()
        }
    }

    /**
     * Create menus on toolbar
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.

        menuInflater.inflate(R.menu.search, menu)

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

        // Handle search. See
        // guide https://developer.android.com/guide/topics/search/
        // API https://developer.android.com/reference/android/support/v7/widget/SearchView

        return super.onCreateOptionsMenu(menu)
    }

    /**
     * Respond to menu item on the toolbar being selected
     */
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
            R.id.action_search -> {
                info("Clicked search")
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
                LoginActivity.startForResult(this, FROM_LOGIN_ACTIVITY)
            }
            R.id.action_sign_up -> {
                SignupActivity.startForResult(this, FROM_SIGNUP_ACTCITITY)
            }
            R.id.action_account -> {
                AccountActivity.start(this)
//                ProfileActivity.start(this)
            }
            R.id.action_subscription -> {

            }
            R.id.action_help -> {

            }
            R.id.action_feedback -> {

            }
            R.id.action_settings -> {
                SettingsActivity.start(this)
            }
            R.id.action_logout -> {
                // Delete user data from shared preference and update UI.
                User.removeFromPref(this)
                updateUIForCookie()
                Toast.makeText(this, "账号已登出", Toast.LENGTH_SHORT).show()
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

    override fun onStartReading(item: ChannelItem) {

        val db = dbHelper?.writableDatabase ?: return

        launch {
            if (readingHistory == null) {
                readingHistory = ReadingHistory(db)
            }

            readingHistory?.add(item)
        }

        Toast.makeText(this, "Saving reading history", Toast.LENGTH_SHORT).show()
    }

    /**
     * Implementation of SectionFragment.OnInAppNavigate.
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

    companion object {
        private const val FROM_LOGIN_ACTIVITY = 1
        private const val FROM_SIGNUP_ACTCITITY = 2
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
            return MyftFragment.newInstance(pages[position].id)
        }

        override fun getCount(): Int {
            return pages.size
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return pages[position].title
        }
    }
}


