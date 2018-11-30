package com.ft.ftchinese

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.ft.ftchinese.models.*
import com.ft.ftchinese.util.*
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.JsonSyntaxException
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.toast

/**
 * This is used to show a channel page, which consists of a list of article summaries.
 * It is similar to `MainActivity` except that it does not wrap a TabLayout.
 */
class ChannelActivity : AppCompatActivity(), AnkoLogger {

    private var mFirebaseAnalytics: FirebaseAnalytics? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_channel)

        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }

        /**
         * Get the metadata for this page of article list
         */
        val data = intent.getStringExtra(EXTRA_PAGE_META)
        try {
            val pageMeta = gson.fromJson<PagerTab>(data, PagerTab::class.java)
            /**
             * Set toolbar's title so that user knows where he is now.
             */
            toolbar.title = pageMeta.title

            var fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

            if (fragment == null) {
                fragment = ViewPagerFragment.newInstance(pageMeta)
                supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .commit()
            }

            mFirebaseAnalytics?.logEvent(FirebaseAnalytics.Event.VIEW_ITEM_LIST, Bundle().apply {
                putString(FirebaseAnalytics.Param.ITEM_CATEGORY, pageMeta.title)
            })

        } catch (e: JsonSyntaxException) {
            toast("$e")
        }
    }

    /**
     * Launch this activity with intent
     */
    companion object {
        private const val EXTRA_PAGE_META = "extra_list_page_metadata"

        fun start(context: Context?, page: PagerTab) {
            val intent = Intent(context, ChannelActivity::class.java).apply {
                putExtra(EXTRA_PAGE_META, gson.toJson(page))
            }

            context?.startActivity(intent)
        }
    }
}
