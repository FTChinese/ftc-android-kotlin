package com.ft.ftchinese

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_chanel.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

/**
 * This is used to show a channel page, which consists of a list of article summaries.
 * It is similar to `MainActivity` execpt that it does not wrap a TabLayout.
 */
class ChannelActivity : AppCompatActivity(), SectionFragment.OnDataLoadListener, SectionFragment.OnInAppNavigate, AnkoLogger {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chanel)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }

        val extraChannel = intent.getStringExtra(EXTRA_CHANNEL_META)

        val sectionFragment = SectionFragment.newInstance(extraChannel)

        supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, sectionFragment)
                .commit()
    }

    override fun onDataLoading() {
        progress_bar.visibility = View.VISIBLE
    }

    override fun onDataLoaded() {
        progress_bar.visibility = View.GONE
    }

    /**
     * This is just a placeholder implementation to prevent `cannot cast` error
     * If user enters this activity, there's no BottomNavigationView
     */
    override fun selectBottomNavItem(itemId: Int) {
        info("Dump implementation")
    }

    override fun selectTabLayoutTab(tabIndex: Int) {
        info("Dump implementation")
    }

    /**
     * Launch this activity with intent
     */
    companion object {
        private const val EXTRA_CHANNEL_META = "channel_metadata"

        fun start(context: Context?, channel: Channel) {
            val intent = Intent(context, ChannelActivity::class.java)
            intent.putExtra(EXTRA_CHANNEL_META, gson.toJson(channel))
            context?.startActivity(intent)
        }
    }
}
