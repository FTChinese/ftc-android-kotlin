package com.ft.ftchinese

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import com.ft.ftchinese.models.*
import com.ft.ftchinese.util.*
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.android.synthetic.main.fragment_channel.*
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast

const val JS_INTERFACE_NAME = "Android"

/**
 * ChannelFragment serves two purposes:
 * As part of TabLayout in MainActivity;
 * As part of ChannelActivity. For example, if you panned to Editor's Choice tab, the mFollows lead to another layer of a list page, not content. You need to use `ChannelFragment` again to render a list page.
 */
class ChannelFragment : Fragment(),
        WVClient.OnWebViewInteractionListener,
        SwipeRefreshLayout.OnRefreshListener,
        AnkoLogger {

    /**
     * Meta data about current page: the tab's title, where to load data, etc.
     * Passed in when the fragment is created.
     */
    private var channelSource: ChannelSource? = null

    private var loadJob: Job? = null
    private var cacheJob: Job? = null
    private var refreshJob: Job? = null

    private lateinit var sessionManager: SessionManager
    private lateinit var cache: FileCache
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    // Hold string in raw/list.html
    private var template: String? = null

    private var articleList: List<ChannelItem>? = null
    private var channelMeta: ChannelMeta? = null

    private fun showProgress(value: Boolean) {
        if (value) {
            progress_bar?.visibility = View.VISIBLE
        } else {
            progress_bar?.visibility = View.GONE
            swipe_refresh?.isRefreshing = false
        }
    }

    /**
     * Bind listeners here.
     */
    override fun onAttach(context: Context) {
        super.onAttach(context)

        sessionManager = SessionManager.getInstance(context)
        cache = FileCache(context)
        firebaseAnalytics = FirebaseAnalytics.getInstance(context)

        info("onAttach finished")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get metadata about current tab
        val channelSourceStr = arguments?.getString(ARG_CHANNEL_SOURCE) ?: return
        channelSource = json.parse<ChannelSource>(channelSourceStr)

        info("Channel source: $channelSource")
        info("onCreate finished")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        super.onCreateView(inflater, container, savedInstanceState)

        info("onCreateView finished")
        return inflater.inflate(R.layout.fragment_channel, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup swipe refresh listener
        swipe_refresh.setOnRefreshListener(this)

        // Configure web view.
        web_view.settings.apply {
            javaScriptEnabled = true
            loadsImagesAutomatically = true
        }

        val wvClient = WVClient(activity)
        wvClient.setOnClickListener(this)

        web_view.apply {

            // Interact with JS.
            // See Page/Layouts/Page/SuperDataViewController.swift#viewDidLoad() how iOS inject js to web view.
            addJavascriptInterface(
                    this@ChannelFragment,
                    JS_INTERFACE_NAME
            )

            // Set WebViewClient to handle various links
            webViewClient = wvClient

            webChromeClient = ChromeClient()
        }

        // Setup back key behavior.
        web_view.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK && web_view.canGoBack()) {
                web_view.goBack()
                return@setOnKeyListener true
            }

            false
        }

        info("Initiating current page with data: $channelSource")

        loadContent()
    }

    override fun onRefresh() {


        // If auto loading did not stop yet, stop it.
        if (loadJob?.isActive == true) {
            loadJob?.cancel()
        }

        when (channelSource?.htmlType) {
            HTML_TYPE_FRAGMENT -> {
                refreshJob = GlobalScope.launch(Dispatchers.Main) {
                    if (template == null) {
                        template = cache.readChannelTemplate()
                    }

                    loadFromServer()
                }
            }
            HTML_TYPE_COMPLETE -> {
                info("onRefresh: reload")
                web_view.reload()
                showProgress(false)
            }
        }
    }

    private fun loadContent() {
        // For partial HTML, we need to crawl its content and render it with a template file to get the template HTML page.
        when (channelSource?.htmlType) {
            HTML_TYPE_FRAGMENT -> {
                info("loadContent: html fragment")

                loadJob = GlobalScope.launch (Dispatchers.Main) {

                    val cacheName = channelSource?.fileName

                    if (cacheName.isNullOrBlank()) {
                        info("Cached file is not found. Fetch content from server")
                        loadFromServer()
                        return@launch
                    }

                    val cachedFrag = withContext(Dispatchers.IO) {
                        cache.loadText(cacheName)
                    }

                    if (cachedFrag.isNullOrBlank()) {
                        info("Cached HTML fragment is not found or empty. Fetch from server")
                        loadFromServer()
                        return@launch
                    }

                    info("Cached HTML fragment is found. Using cache.")
                    loadFromCache(cachedFrag)
                }
            }
            // For complete HTML, load it directly into Web view.
            HTML_TYPE_COMPLETE -> {
                info("loadContent: web page")
                web_view.loadUrl(channelSource?.contentUrl)
            }
        }
    }

    // Load data from cache and update cache in background.
    private suspend fun loadFromCache(htmlFrag: String) {

        if (template == null) {
            template = withContext(Dispatchers.IO) {
                cache.readChannelTemplate()
            }
        }

        renderAndLoad(htmlFrag)

        if (activity?.isActiveNetworkWifi() != true) {
            info("Active network is not wifi. Stop update in background.")
            return
        }

        info("Network is wifi and cached exits. Fetch data to update cache only.")

        val url = channelSource?.contentUrl ?: return

        info("Updating cache in background")

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val data = Fetch().get(url).responseString() ?: return@launch
                cacheData(data)
            } catch (e: Exception) {
                info("Cannot update cache in background. Reason: ${e.message}")
            }
        }
    }

    private suspend fun loadFromServer() {
        if (activity?.isNetworkConnected() != true) {
            showProgress(false)
            toast(R.string.prompt_no_network)
            return
        }

        val url = channelSource?.contentUrl ?: return

        if (template == null) {
            template = withContext(Dispatchers.IO) {
                cache.readChannelTemplate()
            }
        }

        // If this is not refresh action, show progress bar, else show prompt 'Refreshing'
        if (!swipe_refresh.isRefreshing) {
            showProgress(true)
        } else {
            toast(R.string.prompt_refreshing)
        }

        info("Load content from server on webUrl: $url")

        try {
            val data = withContext(Dispatchers.IO) {
                Fetch().get(url).responseString()
            }

            showProgress(false)
            if (data == null) {
                return
            }

            renderAndLoad(data)

            GlobalScope.launch(Dispatchers.IO) {
                cacheData(data)
            }
        } catch (e: Exception) {
            info(e.message)
        }
    }


    private fun cacheData(data: String) {
        val fileName = channelSource?.fileName ?: return

        cache.saveText(fileName, data)
    }

    private fun renderAndLoad(htmlFragment: String) {

        val dataToLoad = channelSource?.render(template, htmlFragment)

        web_view.loadDataWithBaseURL(FTC_OFFICIAL_URL, dataToLoad, "text/html", null, null)

        val fileName = channelSource?.fileName ?: return

        if (dataToLoad != null) {
            cache.saveText("full_$fileName", dataToLoad)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        info("onSaveInstanceState finished")
    }

    override fun onPause() {
        super.onPause()
        info("onPause finished")

        cacheJob?.cancel()
        loadJob?.cancel()
        refreshJob?.cancel()
    }

    override fun onStop() {
        super.onStop()
        info("onStop finished")

        cacheJob?.cancel()
        loadJob?.cancel()
        refreshJob?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()

        cacheJob?.cancel()
        loadJob?.cancel()
        refreshJob?.cancel()

        cacheJob = null
        loadJob = null
        refreshJob = null
    }


    /**
     * WVClient click pagination.
     */
    override fun onPagination(pageKey: String, pageNumber: String) {
        val pageMeta = channelSource ?: return

        val listPage = pageMeta.withPagination(pageKey, pageNumber)

        info("Open a pagination: $listPage")

        if (listPage.shouldReload) {
            info("Reloading a pagination $listPage")

            channelSource = listPage

            loadContent()
        } else {
            info("Start a new activity for $listPage")
            ChannelActivity.start(activity, listPage)
        }
    }

    /**
     * Log when user clicked an article from a list.
     */
    private fun logSelectContent(item: ChannelItem) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, Bundle().apply {
            putString(FirebaseAnalytics.Param.CONTENT_TYPE, item.type)
            putString(FirebaseAnalytics.Param.ITEM_ID, item.id)
        })
    }

    /**
     * Collection data when a web page is loaded into a web view.
     */
    @JavascriptInterface
    fun onPageLoaded(message: String) {

        info("Channel loaded: $message")

        val channelContent = json.parse<ChannelContent>(message) ?: return

        articleList = channelContent.sections[0].lists[0].items
        channelMeta = channelContent.meta

        cacheChannelData(message)
    }

    @JavascriptInterface
    fun onSelectItem(index: String) {
        info("select item: $index")

        val i = try {
            index.toInt()
        } catch (e: Exception) {
            -1
        }

        selectItem(i)
    }

    @JavascriptInterface
    fun onLoadedSponsors(message: String) {

//         See what the sponsor data is.
        if (BuildConfig.DEBUG) {
            val name = channelSource?.name

            if (name != null) {
                info("Saving js posted data for sponsors of $channelSource")
                GlobalScope.launch {
                    cache.saveText("${name}_sponsors.json", message)
                }
            }
        }

        info("Loaded sponsors: $message")

        try {
            SponsorManager.sponsors = json.parseArray(message) ?: return
        } catch (e: Exception) {
            info(e)
        }
    }

    private fun cacheChannelData(data: String) {
        if (!BuildConfig.DEBUG) {
            return
        }

        val fileName = channelSource?.name ?: return

        GlobalScope.launch(Dispatchers.IO) {
            cache.saveText("$fileName.json", data)
        }
    }

    // User click on an item of article list.
    private fun selectItem(index: Int) {
        if (index < 0) {
            return
        }

        val channelItem = articleList
                ?.getOrNull(index)
                ?: return

        info("Selected item: $channelItem")
        
        channelItem.withMeta(channelMeta)
        /**
         * {
         * "id": "007000049",
         * "type": "column",
         * "headline": "徐瑾经济人" }
         * Canonical URL: http://www.ftchinese.com/channel/column.html
         * Content URL: https://api003.ftmailbox.com/column/007000049?webview=ftcapp&bodyonly=yes
         */
        if (channelItem.type == ChannelItem.TYPE_COLUMN) {
            openColumn(channelItem)
            return
        }

        val account = sessionManager.loadAccount()

        val paywallSource = PaywallSource(
                id = channelItem.id,
                category = channelItem.type,
                name = channelItem.title
        )

        when (channelSource?.requiredTier) {
            Tier.STANDARD -> {
                val grant = activity?.shouldGrantStandard(account, paywallSource) ?: return

                if (!grant) {
                    return
                }

                openArticle(channelItem)
                return
            }
            Tier.PREMIUM -> {
                val granted = activity?.shouldGrantPremium(
                        account,
                        paywallSource
                ) ?: return

                if (!granted) {
                    return
                }

                openArticle(channelItem)
                return
            }
        }

        info("Channel source do not require membership")

        if (channelItem.requireStandard()) {
            info("Content required standard member")
            val granted = activity?.shouldGrantStandard(
                    account,
                    paywallSource
            ) ?: return

            if (granted) {
                openArticle(channelItem)
            }

            return
        }

        info("Article do not require standard")

        if (channelItem.requirePremium()) {
            info("Content required premium member")

            val granted = activity?.shouldGrantPremium(
                    account,
                    paywallSource
            ) ?: return

            if (granted) {
                openArticle(channelItem)
            }

            return
        }

        info("Article do not require premium")

        openArticle(channelItem)
    }

    private fun openColumn(item: ChannelItem) {
        val chSrc = ChannelSource(
                title = item.title,
                name = "${item.type}_${item.id}",
                contentUrl = item.buildApiUrl(),
                htmlType = HTML_TYPE_FRAGMENT
        )
        info("Open a column: $chSrc")

        ChannelActivity.start(context, chSrc)
    }

    private fun openArticle(item: ChannelItem) {
        when (item.type) {
            ChannelItem.TYPE_STORY,
            ChannelItem.TYPE_PREMIUM -> {
                ArticleActivity.start(activity, item)
            }
            ChannelItem.TYPE_INTERACTIVE -> {
                when (item.subType) {
                    ChannelItem.SUB_TYPE_RADIO -> {
                        ArticleActivity.start(activity, item)
                    }
                    else -> {
                        ArticleActivity.startWeb(context, item)
                    }
                }
            }
            else -> {
                ArticleActivity.startWeb(context, item)
            }
        }

        logSelectContent(item)
    }

    companion object {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private const val ARG_CHANNEL_SOURCE = "arg_channel_source"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        fun newInstance(channel: ChannelSource) = ChannelFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_CHANNEL_SOURCE, json.toJsonString(channel))
            }
        }

    }
}


