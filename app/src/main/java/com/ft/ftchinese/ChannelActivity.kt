package com.ft.ftchinese

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.ft.ftchinese.models.*
import com.ft.ftchinese.util.json
import com.google.firebase.analytics.FirebaseAnalytics
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
        val pageMeta = json.parse<PagerTab>(data)

        if (pageMeta == null) {
            toast(R.string.prompt_load_failure)
            return
        }
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
    }

    /**
     * Launch this activity with intent
     */
    companion object {
        private const val EXTRA_PAGE_META = "extra_list_page_metadata"

        fun start(context: Context?, page: PagerTab) {
            val intent = Intent(context, ChannelActivity::class.java).apply {
                putExtra(EXTRA_PAGE_META, json.toJsonString(page))
            }

            context?.startActivity(intent)
        }
    }
}
