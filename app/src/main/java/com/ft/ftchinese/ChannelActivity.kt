package com.ft.ftchinese

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.ft.ftchinese.models.PagerTab
import com.ft.ftchinese.models.SessionManager
import com.ft.ftchinese.util.gson
import kotlinx.android.synthetic.main.activity_chanel.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

/**
 * This is used to show a channel page, which consists of a list of article summaries.
 * It is similar to `MainActivity` execpt that it does not wrap a TabLayout.
 */
class ChannelActivity : AppCompatActivity(), ChannelFragment.OnFragmentInteractionListener, ChannelWebViewClient.OnInAppNavigate, AnkoLogger {

    private var mSession: SessionManager? = null

    override fun getSession(): SessionManager? {
        return mSession
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chanel)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }

        mSession = SessionManager.getInstance(this)
        /**
         * Get the metadata for this page of articles
         */
        val data = intent.getStringExtra(EXTRA_LIST_PAGE_META)
        val pageMeta = gson.fromJson<PagerTab>(data, PagerTab::class.java)

        /**
         * Set toolbar's title so that reader know where he is now.
         */
        toolbar.title = pageMeta.title

        /**
         * Begin to attach ChannelFragment to this activity
         */
        val sectionFragment = ChannelFragment.newInstance(pageMeta)

        supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, sectionFragment)
                .commit()
    }

    /**
     * This is just a placeholder implementation to prevent `cannot cast` error
     * If user enters this activity, there's no BottomNavigationView
     */
    override fun selectBottomNavItem(itemId: Int) {

    }

    override fun selectTabLayoutTab(tabIndex: Int) {

    }

    /**
     * Launch this activity with intent
     */
    companion object {
        private const val EXTRA_LIST_PAGE_META = "extra_list_page_metadata"

        fun start(context: Context?, page: PagerTab) {
            val intent = Intent(context, ChannelActivity::class.java)
            intent.putExtra(EXTRA_LIST_PAGE_META, gson.toJson(page))
            context?.startActivity(intent)
        }
    }
}
