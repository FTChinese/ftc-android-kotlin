package com.ft.ftchinese

import android.content.res.Resources
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
import android.util.Log
import android.view.*
import android.support.v7.widget.SearchView
import android.widget.Toast
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

val httpClient = OkHttpClient()
val gson = Gson()

fun requestData(url: String): String? {
    try {
        val request = Request.Builder()
                .url(url)
                .build()
        val response = httpClient.newCall(request).execute()
        return response.body()?.string()
    } catch (e: Exception) {
        Log.w("requestData", e.toString())
    }

    return null
}

fun readHtml(resources: Resources, resId: Int): String? {

    try {
        val input = resources.openRawResource(resId)
        return input.bufferedReader().use { it.readText() }

    } catch (e: ExceptionInInitializerError) {
        Log.e("readHtml", e.toString())
    }
    return null
}


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, TabLayout.OnTabSelectedListener, SectionFragment.OnDataLoadListener, SectionFragment.OnInAppNavigate, AnkoLogger {

    /**
     * Implementation of BottomNavigationView.OnNavigationItemSelectedListener
     */
    private val bottomNavItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
       info("Selected bottom nav item ${item.title}")

        when (item.itemId) {
            R.id.nav_news -> {
                supportActionBar?.setTitle(R.string.app_name)
                container.adapter = SectionsPagerAdapter(newsChannels, supportFragmentManager)

            }

            R.id.nav_english -> {
                supportActionBar?.setTitle(R.string.nav_english)
                container.adapter = SectionsPagerAdapter(englishChannels, supportFragmentManager)

            }

            R.id.nav_ftacademy -> {
                supportActionBar?.setTitle(R.string.nav_ftacademy)
                container.adapter = SectionsPagerAdapter(ftaChannels, supportFragmentManager)

            }

            R.id.nav_video -> {
                supportActionBar?.setTitle(R.string.nav_video)
                container.adapter = SectionsPagerAdapter(videoChannels, supportFragmentManager)

            }

            R.id.nav_myft -> {
                info("Selected bottom nav item ${item.title}")
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

        // Set a listener that will be notified when a menu item is selected.
        nav_view.setNavigationItemSelectedListener(this)

        // Set ViewPager adapter
        container.adapter = SectionsPagerAdapter(newsChannels, supportFragmentManager)

        // Link ViewPager and TabLayout
        tab_layout.setupWithViewPager(container)
        tab_layout.addOnTabSelectedListener(this)

        // Bottom navigation listener
        bottom_nav.setOnNavigationItemSelectedListener(bottomNavItemSelectedListener)
        bottom_nav.setOnNavigationItemReselectedListener(bottomNavItemReseletedListener)

    }

    override fun onStart() {
        super.onStart()

    }

    override fun onResume() {
        super.onResume()

    }

    override fun onPause() {
        super.onPause()

    }

    override fun onStop() {
        super.onStop()

    }

    override fun onRestart() {
        super.onRestart()

    }

    override fun onDestroy() {
        super.onDestroy()

    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)

    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {

        super.onRestoreInstanceState(savedInstanceState)

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

        menuInflater.inflate(R.menu.main, menu)

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
     * Implements NavigationView.OnNavigationItemSelectedListener
     */
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        Toast.makeText(this, "You selected navigation item ${item.itemId}", Toast.LENGTH_SHORT).show()

        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_camera -> {
                // Handle the camera action
            }
            R.id.nav_gallery -> {

            }
            R.id.nav_slideshow -> {

            }
            R.id.nav_manage -> {

            }
            R.id.nav_share -> {

            }
            R.id.nav_send -> {

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
    override fun onDataLoaded() {
        progress_bar.visibility = View.GONE
    }

    override fun onDataLoading() {
        progress_bar.visibility = View.VISIBLE
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

    /**
     * A [FragmentPagerAdapter] that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    inner class SectionsPagerAdapter(private val channels: Array<Channel>, fm: FragmentManager) : FragmentStatePagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            info("Fragment data: ${channels[position]}")
            return SectionFragment.newInstance(gson.toJson(channels[position]))
        }

        override fun getCount(): Int {
            // Show 3 total pages.
            return channels.size
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return channels[position].title
        }
    }

}
