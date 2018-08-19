package com.ft.ftchinese

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.ft.ftchinese.models.ChannelItem
import com.ft.ftchinese.models.ListPage
import com.ft.ftchinese.util.gson
import kotlinx.android.synthetic.main.activity_chanel.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

/**
 * This is used to show a channel page, which consists of a list of article summaries.
 * It is similar to `MainActivity` execpt that it does not wrap a TabLayout.
 */
class ChannelActivity : AppCompatActivity(), SectionFragment.OnFragmentInteractionListener, ChannelWebViewClient.OnInAppNavigate, AnkoLogger {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chanel)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }

        /**
         * Get the metadata for this page of articles
         */
        val pageMeta = intent.getStringExtra(EXTRA_LIST_PAGE_META)
        val listPage = gson.fromJson<ListPage>(pageMeta, ListPage::class.java)

        /**
         * Set toolbar's title so that reader know where he is now.
         */
        toolbar.title = listPage.title

        /**
         * Begin to attach SectionFragment to this activity
         */
        val sectionFragment = SectionFragment.newInstance(listPage)

        supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, sectionFragment)
                .commit()
    }

    override fun onProgress(show: Boolean) {
        if (show) {
            progress_bar.visibility = View.VISIBLE
        } else {
            progress_bar.visibility = View.GONE
        }
    }

    override fun onStartReading(item: ChannelItem) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
        private const val EXTRA_LIST_PAGE_META = "extra_list_page_metadata"

        fun start(context: Context?, page: ListPage) {
            val intent = Intent(context, ChannelActivity::class.java)
            intent.putExtra(EXTRA_LIST_PAGE_META, gson.toJson(page))
            context?.startActivity(intent)
        }
    }
}
