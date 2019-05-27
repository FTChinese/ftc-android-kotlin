package com.ft.ftchinese

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import com.ft.ftchinese.base.ScopedFragment
import com.ft.ftchinese.base.isNetworkConnected
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
@kotlinx.coroutines.ExperimentalCoroutinesApi
class ChannelFragment : ScopedFragment(),
        WVClient.OnWebViewInteractionListener,
        SwipeRefreshLayout.OnRefreshListener,
        AnkoLogger {

    /**
     * Meta data about current page: the tab's title, where to load data, etc.
     * Passed in when the fragment is created.
     */
    private var channelSource: ChannelSource? = null
    private var listUrl: String = ""

    private lateinit var sessionManager: SessionManager
    private lateinit var cache: FileCache
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private var loadingJob: Job? = null

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

        val targetUrl = channelSource?.contentUrl ?: return

        val queryValue = flavorQuery[BuildConfig.FLAVOR]

        listUrl = if (queryValue == null) {
            targetUrl
        } else {
            try {
                Uri.parse(targetUrl).buildUpon()
                        .appendQueryParameter("utm_source", "marketing")
                        .appendQueryParameter("utm_mediu", "androidmarket")
                        .appendQueryParameter("utm_campaign", queryValue)
                        .appendQueryParameter("android", BuildConfig.VERSION_CODE.toString(10))
                        .build()
                        .toString()
            } catch (e: Exception) {
                targetUrl
            }
        }

        if (BuildConfig.DEBUG) {
            info("List url: $listUrl")
        }
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
            domStorageEnabled = true
            databaseEnabled = true
        }

        val wvClient = WVClient(activity)
        wvClient.setWVInteractionListener(this)

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

        initLoading()
    }

    override fun onRefresh() {


        // If auto loading did not stop yet, stop it.
        if (loadingJob?.isActive == true) {
            loadingJob?.cancel()
        }

        toast(R.string.prompt_refreshing)

        when (channelSource?.htmlType) {
            HTML_TYPE_FRAGMENT -> {
                launch {
                    info("start refreshing: html fragment")
                    loadFromServer()
                    toast(R.string.prompt_updated)
                }
            }
            HTML_TYPE_COMPLETE -> {
                info("start refreshing: reload")
                web_view.reload()
                showProgress(false)
                toast(R.string.prompt_updated)
            }
        }
    }

    private fun initLoading() {
        showProgress(true)

        // For partial HTML, we need to crawl its content and render it with a template file to get the template HTML page.
        when (channelSource?.htmlType) {
            HTML_TYPE_FRAGMENT -> {
                info("initLoading: html fragment")

                loadingJob = launch {

                    val cachedFrag = loadFromCache()

                    // If local cache is found.
                    if (cachedFrag != null) {
                        // use cache
                        val html = render(cachedFrag)

                        // If local cache could be rendered, use it and fetch latest remote data silently.
                        if (html != null) {
                            load(html)

                            if (activity?.isNetworkConnected() == true) {
                                fetchAndCacheRemote()
                            }

                            return@launch
                        }

                        // If local cache cannot be rendered, fetch server data.
                        loadFromServer()
                    }

                    loadFromServer()
                }
            }
            // For complete HTML, load it directly into Web view.
            HTML_TYPE_COMPLETE -> {
                info("initLoading: web page")
                web_view.loadUrl(listUrl)
                showProgress(false)
            }
        }
    }

    private suspend fun loadFromCache(): String? {
        val cacheName = channelSource?.fileName
        if (cacheName.isNullOrBlank()) {
            return null
        }

        val cachedFrag = withContext(Dispatchers.IO) {
            cache.loadText(cacheName)
        }

        if (cachedFrag.isNullOrBlank()) {
            return null
        }

        return cachedFrag
    }

    private suspend fun loadFromServer() {
        if (activity?.isNetworkConnected() != true) {
            showProgress(false)
            toast(R.string.prompt_no_network)
            return
        }

        if (listUrl.isEmpty()) {
            showProgress(false)
            toast("Target URL is not found")
            return
        }

        val remoteFrag = fetchAndCacheRemote()

        if (remoteFrag.isNullOrBlank()) {
            showProgress(false)
            toast(R.string.api_server_error)
            return
        }

        val html = render(remoteFrag) ?: return
        load(html)
    }

    private suspend fun fetchAndCacheRemote(): String? = withContext(Dispatchers.IO) {
        if (BuildConfig.DEBUG) {
            info("Fetching data from $listUrl")
        }

        if (listUrl.isEmpty()) {
            return@withContext null
        }
        val remoteFrag = try {
            Fetch().get(listUrl).responseString()
        } catch (e: Exception) {
            null
        }

        if (!remoteFrag.isNullOrBlank()) {
            launch(Dispatchers.IO) {
                cacheData(remoteFrag)
            }
        }
        remoteFrag
    }

    private fun cacheData(data: String) {

        val fileName = channelSource?.fileName ?: return

        if (BuildConfig.DEBUG) {
            info("Caching data to $fileName")
        }

        cache.saveText(fileName, data)
    }

    // Since the render process is over-complicated, move it to background.
    private suspend fun render(htmlFragment: String): String? = withContext(Dispatchers.Default) {

        if (template == null) {
            template = cache.readChannelTemplate()
        }

        channelSource?.render(template, htmlFragment)
    }

    private fun load(html: String) {
        if (BuildConfig.DEBUG) {
            info("Loading web page to web view")
        }
        web_view.loadDataWithBaseURL(FTC_OFFICIAL_URL, html, "text/html", null, null)

        showProgress(false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        info("onSaveInstanceState finished")
    }

    override fun onPause() {
        super.onPause()
        info("onPause finished")
        cancel()
    }

    override fun onStop() {
        super.onStop()
        info("onStop finished")
        cancel()
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

            initLoading()
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

        info("Select item: $channelItem")

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

        info("Is channel require membership: $channelSource")

        // Handle standard subscription and premium subscription channel
        when (channelSource?.requiredTier) {
            Tier.STANDARD -> {
                val grant = activity?.shouldGrantStandard(account) ?: return

                if (!grant) {
                    PaywallTracker.fromArticle(channelItem)

                    return
                }

                openArticle(channelItem)
                return
            }
            Tier.PREMIUM -> {
                val granted = activity?.shouldGrantPremium(account) ?: return

                if (!granted) {
                    PaywallTracker.fromArticle(channelItem)

                    return
                }

                openArticle(channelItem)
                return
            }
        }

        info("Channel source do not require membership")

        if (channelItem.isFree()) {
            info("Open a free article")
            openArticle(channelItem)

            return
        }

        info("Content requires membership")
        if (channelItem.requirePremium()) {
            info("Content restricted to premium members")
            val granted = activity?.shouldGrantPremium(account) ?: return
            if (granted) {
                openArticle(channelItem)
            } else {
                PaywallTracker.fromArticle(channelItem)
            }

            return
        }

        info("Content restricted to standard members")
        val granted = activity?.shouldGrantStandard(account) ?: return

        if (granted) {
            openArticle(channelItem)
        } else {
            PaywallTracker.fromArticle(channelItem)
        }
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
        info("Open article for an channel item: $item")

        when (item.type) {
            ChannelItem.TYPE_STORY,
            ChannelItem.TYPE_PREMIUM -> {
                ArticleActivity.start(activity, item)
            }
            ChannelItem.TYPE_INTERACTIVE -> {
                when (item.subType) {
                    ChannelItem.SUB_TYPE_RADIO -> {
                        ArticleActivity.startWeb(activity, item)
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


