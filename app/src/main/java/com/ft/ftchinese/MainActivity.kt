package com.ft.ftchinese

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.design.widget.NavigationView
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
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch

val newsChannels = arrayOf("首页", "中国", "全球", "经济", "金融市场", "商业", "创新经济", "教育", "观点", "管理", "生活时尚")
val englishChannels = arrayOf("英语电台", "金融英语速读", "双语阅读", "原声视频")
val ftaChannels = arrayOf("商学院观察", "热点观察", "MBA训练营", "互动小测", "深度阅读")
val videoChannels = arrayOf("最新", "政经", "商业", "秒懂", "金融", "文化", "高端视点", "有色眼镜")

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, ContentFragment.OnDataLoadListener {

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

        container.adapter = SectionsPagerAdapter(newsChannels, supportFragmentManager)

        // Link ViewPager and TabLayout
        tab_layout.setupWithViewPager(container)

        // Bottom navigation listener
        bottom_nav.setOnNavigationItemSelectedListener(bottomNavItemSelectedListener)
        bottom_nav.setOnNavigationItemReselectedListener(bottomNavItemReseletedListener)

        Log.i(tag, "fileList: ${fileList()}")

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
    inner class SectionsPagerAdapter(private val channels: Array<String>, fm: FragmentManager) : FragmentStatePagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return ContentFragment.newInstance()
        }

        override fun getCount(): Int {
            // Show 3 total pages.
            return channels.size
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return channels[position]
        }
    }

}
