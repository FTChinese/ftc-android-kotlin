package com.ft.ftchinese

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.os.PersistableBundle
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
import android.widget.Toast
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import okhttp3.OkHttpClient
import okhttp3.Request

val httpClient = OkHttpClient()
val gson = Gson()

suspend fun requestData(url: String): String? {
    try {
        val request = Request.Builder()
                .url(url)
                .build()
        val response = httpClient.newCall(request).execute()
        return response.body()?.string()
    } catch (e: Exception) {
        Log.e("requestData", e.toString())
    }

    return null
}

suspend fun readHtml(resources: Resources, resId: Int): String? {

    try {
        val input = resources.openRawResource(resId)
        return input.bufferedReader().use { it.readText() }

    } catch (e: ExceptionInInitializerError) {
        Log.e("readHtml", e.toString())
    }
    return null
}

const val TAB_INDEX_KEY = "tab_index"

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, TabLayout.OnTabSelectedListener, ContentFragment.OnDataLoadListener {

    private val tag = "MainActivity"

    private val bottomNavItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        Log.i(tag, "Selected bottom nav item ${item.title}")

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
                Log.i(tag, "Selected bottom nav item ${item.title}")
            }
        }
        true
    }

    private val bottomNavItemReseletedListener = BottomNavigationView.OnNavigationItemReselectedListener { item ->
        Log.i(tag, "Reselected bottom nav item: ${item.title}")
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

        Log.i(tag, "Activity start")
    }

    override fun onResume() {
        super.onResume()

        Log.i(tag, "Activity resume")
    }

    override fun onPause() {
        super.onPause()

        Log.i(tag, "Activity pause")
    }

    override fun onStop() {
        super.onStop()

        Log.i(tag, "Activity stop")
    }

    override fun onRestart() {
        super.onRestart()

        Log.i(tag, "Activity restart")
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.i(tag, "Activity destroy")
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)

        Log.i(tag, "Saving instance state: selected tab position is ${tab_layout.selectedTabPosition}")

    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {

        super.onRestoreInstanceState(savedInstanceState)

        Log.i(tag, "Restoring instance state")
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_settings -> return true
            else -> return super.onOptionsItemSelected(item)
        }
    }

    // Implements NavigationView.OnNavigationItemSelectedListener
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

    override fun onTabSelected(tab: TabLayout.Tab?) {
        Log.i(tag, "Tab position: ${tab?.position}")
        Log.i(tag, "Tab selected: ${tab_layout.selectedTabPosition}")
    }

    override fun onTabReselected(tab: TabLayout.Tab?) {
        Log.i(tag, "Tab reselected: ${tab?.position}")
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) {
        Log.i(tag, "Tab unselected: ${tab?.position}")
    }

    override fun onDataLoaded() {
        progress_bar.visibility = View.GONE
    }

    override fun onDataLoading() {
        progress_bar.visibility = View.VISIBLE
    }

    /**
     * A [FragmentPagerAdapter] that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    inner class SectionsPagerAdapter(private val channels: Array<Channel>, fm: FragmentManager) : FragmentStatePagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            Log.i(tag, "Fragment data: ${channels[position]}")
            return ContentFragment.newInstance(gson.toJson(channels[position]))
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
