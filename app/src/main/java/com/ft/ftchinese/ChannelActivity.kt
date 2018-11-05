package com.ft.ftchinese

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.view.View
import awaitStringResponse
import awaitStringResult
import com.ft.ftchinese.models.*
import com.ft.ftchinese.util.Store
import com.ft.ftchinese.util.gson
import com.ft.ftchinese.util.isNetworkConnected
import com.github.kittinunf.fuel.Fuel
import kotlinx.android.synthetic.main.activity_chanel.*
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

/**
 * This is used to show a channel page, which consists of a list of article summaries.
 * It is similar to `MainActivity` execpt that it does not wrap a TabLayout.
 */
class ChannelActivity : AppCompatActivity(),
        SwipeRefreshLayout.OnRefreshListener,
        JSInterface.OnEventListener,
        AnkoLogger {

    // Passed from caller
    private var mPageMeta: PagerTab? = null
    // Passed from JS
    private var mChannelItems: Array<ChannelItem>? = null
    // Passed from JS
    private var mChannelMeta: ChannelMeta? = null

    private var mLoadJob: Job? = null
    private var mRefreshJob: Job? = null
    private var mSession: SessionManager? = null

    // Content in raw/list.html
    private var mTemplate: String? = null

    private var isInProgress: Boolean = false
        set(value) {
            if (value) {
                progress_bar.visibility = View.VISIBLE
            } else {
                progress_bar.visibility = View.GONE
                swipe_refresh.isRefreshing = false
            }
        }

    override fun onPageLoaded(channelDate: ChannelContent) {
        mChannelItems = channelDate.sections[0].lists[0].items
        mChannelMeta = channelDate.meta
    }

    override fun onSelectItem(index: Int) {
        val channelMeta = mChannelMeta ?: return
        val channelItem = mChannelItems?.getOrNull(index) ?: return

        channelItem.channelTitle = channelMeta.title
        channelItem.theme = channelMeta.theme
        channelItem.adId = channelMeta.adid
        channelItem.adZone = channelMeta.adZone

        if (!channelItem.isMembershipRequired) {

            return
        }
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

        try {
            val pageMeta = gson.fromJson<PagerTab>(data, PagerTab::class.java)

            /**
             * Set toolbar's title so that reader know where he is now.
             */
            toolbar.title = pageMeta.title

            web_view.apply {
                addJavascriptInterface(
                        JSInterface(pageMeta).apply {
                            setOnEventListener(this@ChannelActivity)
                        },
                        JS_INTERFACE_NAME
                )

                webViewClient = ChannelWebViewClient(this@ChannelActivity)
                webChromeClient = MyChromeClient()
            }

            mPageMeta = pageMeta
            info("Initiating current page with data: $mPageMeta")

            loadHtmlFragment(false)
        } catch (e: Exception) {
            info("$e")

            return
        }
    }

    private fun loadHtmlFragment(isRefresh: Boolean) {
        if (!isNetworkConnected()) {
            toast(R.string.prompt_no_network)
            return
        }
        mLoadJob = GlobalScope.launch(Dispatchers.Main) {
            if (mTemplate == null) {
                mTemplate = Store.readChannelTemplate(resources) ?: return@launch
            }

            info("Template: $mTemplate")

            if (!isRefresh) {
                info("Show progress")
                isInProgress = true
            }

            val url = mPageMeta?.contentUrl ?: return@launch

            Fuel.get(url)
                    .awaitStringResult()
                    .fold(
                            { data ->
                                isInProgress = false

                                val fullHtml = mPageMeta?.render(mTemplate, data)

                                web_view.loadDataWithBaseURL(WEBVIEV_BASE_URL, fullHtml, "text/html", null, null)
                            },
                            { error ->
                                isInProgress = false

                                toast("Data not found")
                            }
                    )

//            try {
//
//                val listFrag = mPageMeta?.crawlWeb()
//
//                isInProgress = false
//
//                info("List fragment: $listFrag")
//
//                if (listFrag == null) {
//                    toast("No data found")
//
//                    return@launch
//                }
//
//                val fullHtml = mPageMeta?.render(mTemplate, listFrag)
//
//                web_view.loadDataWithBaseURL(WEBVIEV_BASE_URL, fullHtml, "text/html", null, null)
//
//            } catch (e: Exception) {
//                e.printStackTrace()
//
//                toast(e.toString())
//            }

        }
    }

    override fun onRefresh() {
        toast(R.string.prompt_refreshing)

        if (!isNetworkConnected()) {
            toast(R.string.prompt_no_network)
            return
        }

        if (mLoadJob?.isActive == true) {
            mLoadJob?.cancel()
        }

        loadHtmlFragment(true)
    }

    override fun onStop() {
        super.onStop()
        mLoadJob?.cancel()
        mRefreshJob?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        mLoadJob?.cancel()
        mRefreshJob?.cancel()
    }

    /**
     * Launch this activity with intent
     */
    companion object {
        private const val WEBVIEV_BASE_URL = "http://www.ftchinese.com"
        private const val EXTRA_LIST_PAGE_META = "extra_list_page_metadata"

        fun start(context: Context?, page: PagerTab) {
            val intent = Intent(context, ChannelActivity::class.java).apply {
                putExtra(EXTRA_LIST_PAGE_META, gson.toJson(page))
            }

            context?.startActivity(intent)
        }
    }
}
