package com.ft.ftchinese

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.support.v4.widget.SwipeRefreshLayout
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.ft.ftchinese.models.*
import com.ft.ftchinese.util.Store
import com.ft.ftchinese.util.gson
import com.ft.ftchinese.util.isActiveNetworkWifi
import com.ft.ftchinese.util.isNetworkConnected
import com.koushikdutta.ion.Ion
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
 * Implements JSInterface.OnEventListener to handle events
 * in a web page.
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
         * Get the metadata for this page of article list
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
                        // Set JS event listener to current class.
                        JSInterface(pageMeta).apply {
                            setOnEventListener(this@ChannelActivity)
                        },
                        JS_INTERFACE_NAME
                )

                webViewClient = ChannelWVClient()
                webChromeClient = ChromeClient()
            }

            mPageMeta = pageMeta
            info("Initiating current page with data: $mPageMeta")

            loadContent()

        } catch (e: Exception) {
            info("$e")

            return
        }
    }

    private fun loadContent() {
        when (mPageMeta?.htmlType) {
            PagerTab.HTML_TYPE_FRAGMENT -> {
                info("loadContent: html fragment")

                mLoadJob = GlobalScope.launch(Dispatchers.Main) {
                    val cachedFrag = Store.load(this@ChannelActivity, mPageMeta?.fileName)

                    if (cachedFrag != null) {
                        loadFromCache(cachedFrag)

                        return@launch
                    }

                    loadFromServer()
                }
            }

            PagerTab.HTML_TYPE_COMPLETE -> {
                web_view.loadUrl(mPageMeta?.contentUrl)
            }
        }
    }

    private suspend  fun loadFromCache(htmlFrag: String) {
        if (mTemplate == null) {
            mTemplate = Store.readChannelTemplate(resources)
        }

        renderAndLoad(htmlFrag)

        if (!isActiveNetworkWifi()) {
            return
        }

        info("Loaded data from cache. Network on wifi and update cache in background")

        val url = mPageMeta?.contentUrl ?: return

        // Launch in background.
        mLoadJob = GlobalScope.launch {
            try {
                val frag = Ion.with(this@ChannelActivity)
                        .load(url)
                        .asString()
                        .get()
                Store.save(this@ChannelActivity, mPageMeta?.fileName, frag)
            } catch (e: Exception) {
                info("Error fetch data. Reason: $e")
            }
        }
    }

    private suspend fun loadFromServer() {
        if (!isNetworkConnected()) {
            toast(R.string.prompt_no_network)

            return
        }

        info("Cache not found. Fetch from remote ${mPageMeta?.contentUrl}")

        val url = mPageMeta?.contentUrl ?: return

        if (mTemplate == null) {
            mTemplate = Store.readChannelTemplate(resources)
        }

        if (!swipe_refresh.isRefreshing) {
            isInProgress = true
        }

        Ion.with(this)
                .load(url)
                .asString()
                .setCallback { e, result ->
                    isInProgress = false

                    if (e != null) {
                        info("Failed to fetch $url. Reason $e")
                        return@setCallback
                    }

                    renderAndLoad(result)

                    cacheData(result)
                }
    }

    private fun cacheData(data: String) {
        GlobalScope.launch {
            info("Caching data to file: ${mPageMeta?.fileName}")

            Store.save(this@ChannelActivity, mPageMeta?.fileName, data)
        }

    }
    private fun renderAndLoad(htmlFragment: String) {
        val dataToLoad = mPageMeta?.render(mTemplate, htmlFragment)

        web_view.loadDataWithBaseURL(WEBVIEV_BASE_URL, dataToLoad, "text/html", null, null)
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

        mRefreshJob = GlobalScope.launch(Dispatchers.Main) {
            loadFromServer()
        }
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

        mLoadJob = null
        mRefreshJob = null
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

    /**
     * Handles URL click event for contents loaded into ChannelActivity.
     * This the main difference between this one and
     * MainWebViewClient lies in how it handles pagniation:
     * When user clicked a pagination link, MainWebViewClient
     * starts a new ChannelActivity; but this one simply load new
     * HTML string into web view.
     */
    inner class ChannelWVClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val uri = request?.url ?: return true

            return when (uri.scheme) {
                "http", "https" -> {
                    if (Endpoints.hosts.contains(uri.host)) {
                        return handleInSiteLink(uri)
                    }

                    return handleExternalLink(uri)
                }

                else -> true
            }
        }



        private fun handleInSiteLink(uri: Uri): Boolean {

            if (uri.getQueryParameter("page") != null || uri.getQueryParameter("p") != null) {
                info("Reuse channel activity by reloading a new page")
                return  openPanigation(uri)
            }
            return true
        }

        // Handles clicks on a pagination link.
        // This action simply changes the mPageMeta and force webview to load new html content by crawling a new web page.
        // No new activity or fragment is created.
        private fun openPanigation(uri: Uri): Boolean {
            val currentPage = mPageMeta ?: return true

            val pageNumber = uri.getQueryParameter("page")
                    ?: uri.getQueryParameter("p")
                    ?: return true

            val nameArr = currentPage.name.split("_").toMutableList()
            if (nameArr.size > 0) {
                nameArr[nameArr.size - 1] = pageNumber
            }

            val newName = nameArr.joinToString("_")

            val currentUri = Uri.parse(currentPage.contentUrl)
            val url = uri.buildUpon()
                    .scheme(currentUri.scheme)
                    .authority(currentUri.authority)
                    .path(currentUri.path)
                    .appendQueryParameter("bodyonly", "yes")
                    .appendQueryParameter("webview", "ftcapp")
                    .build()
                    .toString()


            mPageMeta = PagerTab(
                    title = currentPage.title,
                    name = newName,
                    contentUrl = url,
                    htmlType = currentPage.htmlType
            )

            info("Loading pagination url: ${mPageMeta?.contentUrl}")
            loadContent()

            return true
        }

        // Open an external link in browser
        private fun handleExternalLink(uri: Uri): Boolean {
            // This opens an external browser
            val customTabsInt = CustomTabsIntent.Builder().build()
            customTabsInt.launchUrl(this@ChannelActivity, uri)

            return true
        }
    }
}
