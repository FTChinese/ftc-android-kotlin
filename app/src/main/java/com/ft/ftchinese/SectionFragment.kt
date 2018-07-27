package com.ft.ftchinese

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import kotlinx.android.synthetic.main.fragment_section.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.warn


/**
 * SectionFragment serves two purposes:
 * As part of TabLayout in MainActivity;
 * As part of ChannelActivity. For example, if you panned to Editor's Choice tab, the items lead to another layer of a list page, not content. You need to use `SectionFragment` again to render a list page.
 */
class SectionFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener, AnkoLogger {

    private lateinit var listener: OnDataLoadListener
    private lateinit var navigateListener: ChannelWebViewClient.OnInAppNavigate
    private lateinit var mWebViewClient: ChannelWebViewClient

    /**
     * Meta data about current page: the tab's title, where to load data, etc.
     * Passed in when the fragment is created.
     */
    private var currentPage: ListPage? = null

    /**
     * iOS equivalent might be defined Page/Layouts/Pages/Content/DetailModelController.swift#pageData
     * This is a list of articles on each currentPage.
     * Its value if set when WebView finished loading a web page
     */
    private var channelItems: Array<ChannelItem>? = null
    private var channelMeta: ChannelMeta? = null

    // Containing activity should implement this interface to show progress state
    interface OnDataLoadListener {
        fun onDataLoaded()

        fun onDataLoading()
    }

    /**
     * Trigger a menu item in bottom navigation to be selected.
     * This is used to handle links on front page which actually should jump to a bottom navigation item.
     */
//    interface OnInAppNavigate {
//        fun selectBottomNavItem(itemId: Int)
//
//        fun selectTabLayoutTab(tabIndex: Int)
//    }

    companion object {

        private const val WEBVIEV_BASE_URL = "http://www.ftchinese.com"
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private const val ARG_SECTION_PAGE = "arg_section_page"
        private val HTML_PLACEHOLDER = """
            <html>
                <body>
                    <h1>无法加载数据</h1>
                </body>
            </html>
        """.trimIndent()

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        fun newInstance(page: ListPage): SectionFragment {
            val fragment = SectionFragment()
            val args = Bundle()
            args.putString(ARG_SECTION_PAGE, gson.toJson(page))
            fragment.arguments = args
            return fragment
        }
    }

    /**
     * Bind listeners here.
     */
    override fun onAttach(context: Context?) {
        info("onAttach fragment")
        super.onAttach(context)
        // Cast parent activity
        listener = context as OnDataLoadListener
        navigateListener = context as ChannelWebViewClient.OnInAppNavigate
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pageMetadata = arguments?.getString(ARG_SECTION_PAGE)

        currentPage = gson.fromJson<ListPage>(pageMetadata, ListPage::class.java)

        mWebViewClient = ChannelWebViewClient(context, currentPage)
        mWebViewClient.setOnInAppNavigateListener(navigateListener)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

//        val rootView = inflater.inflate(R.layout.fragment_section, container, false)
//        You need to import kotlinx.android.synthetic.main.fragment_section.view.* if you want to access child view here:
//        rootView.section_label.text = getString(R.string.section_format, arguments?.getInt(ARG_SECTION_NUMBER))
//        return rootView

        super.onCreateView(inflater, container, savedInstanceState)

        return inflater.inflate(R.layout.fragment_section, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipe_refresh_layout.setOnRefreshListener(this)

        web_view.settings.apply {
            javaScriptEnabled = true
            loadsImagesAutomatically = true
        }

        web_view.apply {

            // See Page/Layouts/Page/SuperDataViewController.swift#viewDidLoad() how iOS inject js to web view.
            addJavascriptInterface(WebAppInterface(), "Android")
            // Set WebViewClient to handle various links
            webViewClient = mWebViewClient
            webChromeClient = MyChromeClient()
        }

        web_view.setOnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && web_view.canGoBack()) {
                web_view.goBack()
                return@setOnKeyListener true
            }

            false
        }

        info("Initiating data...")

        init()

        info("Finish onCreateView")
    }


    override fun onRefresh() {
        Toast.makeText(context, "Refreshing", Toast.LENGTH_SHORT).show()

        launch(UI) {
            fetchAndUpdate()
        }
    }

    private fun init() {

        showProgress()

        if (currentPage?.listUrl != null ) {

            launch (UI) {
                if (currentPage?.name != null) {
                    val readCacheResult = async { Store.load(context, "${currentPage?.name}.html") }
                    val cachedHtml = readCacheResult.await()

                    if (cachedHtml != null) {
                        info("Using cached data for ${currentPage?.name}")

                        updateUi(cachedHtml)

                        return@launch
                    }
                }

                fetchAndUpdate()
            }

            return
        }

        if (currentPage?.webUrl != null) {
            web_view.loadUrl(currentPage?.webUrl)

            stopProgress()
        }

    }

    private suspend fun fetchAndUpdate() {
        val readResult = async { readHtml(resources, R.raw.list) }
        info("Fetch currentPage data ${currentPage?.listUrl}")

        val fetchResult = async { Request.get(currentPage?.listUrl!!) }

        val templateHtml = readResult.await()
        val remoteHtml = fetchResult.await()

        if (templateHtml == null || remoteHtml == null) {
            updateUi(HTML_PLACEHOLDER)
            return
        }


        val htmlString = templateHtml.replace("{list-content}", remoteHtml)

        updateUi(htmlString)

        // Cache file

        if (currentPage?.name != null) {
            async { Store.save(context, "${currentPage?.name}.html", htmlString) }
        }
    }

    private fun updateUi(data: String) {
        web_view.loadDataWithBaseURL(WEBVIEV_BASE_URL, data, "text/html", null, null)
        stopProgress()
    }

    private fun showProgress() {
        listener.onDataLoading()
    }

    private fun stopProgress() {
        listener.onDataLoaded()
        swipe_refresh_layout.isRefreshing = false
    }

//    inner class SectionWebViewClient : WebViewClient(), AnkoLogger {
//
//        override fun onLoadResource(view: WebView?, url: String?) {
//            super.onLoadResource(view, url)
////            info("Will loading resource $url")
//        }
//
//        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
//            super.onPageStarted(view, url, favicon)
////            info("Page started loading: $url")
//        }
//
//        override fun onPageFinished(view: WebView?, url: String?) {
//            super.onPageFinished(view, url)
//        }
//
//        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
//            super.onReceivedError(view, request, error)
//            warn("Failed to ${request?.method}: ${request?.url}. ${error.toString()}")
//        }
//
//        override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
//            super.onReceivedHttpError(view, request, errorResponse)
//
//            warn("HTTP error - ${request?.method}: ${request?.url}")
//        }
//
//        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
//            info("shouldOverrideUrlLoading: $url")
//
//            if (url == null) {
//                return false
//            }
//            val uri = Uri.parse(url)
//
//            info("Path segments: ${uri.pathSegments}")
//
//            if (uri.host == "www.ftchinese.com") {
//                info("Handle in site links")
//                return handleInSiteLink(uri)
//            }
//
//            info("Handle external links")
//            return handleExternalLink(uri)
//        }
//
//        private fun handleInSiteLink(uri: Uri): Boolean {
//
//            val pathSegments = uri.pathSegments
//
//
//            if (pathSegments.size < 2) {
//                /**
//                 * Handle the pagination link of each channel
//                 * There's a problem with each channel's pagination: they used relative urls.
//                 * When loaded in WebView with base url `http://www.ftchinese.com`,
//                 * the url will become something `http://www.ftchinese.com/china.html?page=2`,
//                 * which should actually be `http://www.ftchiese.com/channel/china.html?page=2`
//                 */
//                val queryPage = uri.getQueryParameter("page")
//                if (queryPage != null) {
//                    val page = ListPage(
//                            title = currentPage?.title ?: "",
//                            name = "channel_p${queryPage}_${uri.lastPathSegment}",
//                            listUrl = buildUrl(uri, "/channel/${uri.path}")
//                    )
//
//                    /**
//                     * Start a new page of article list.
//                     */
//                    ChannelActivity.start(context, page)
//                    return true
//                }
//
//                /**
//                 * Assume this is a content page and load the url directly.
//                 */
//                ContentActivity.start(context, buildUrl(uri))
//                return true
//            }
//
//
//            when (pathSegments[0]) {
//
//                "channel" -> {
//                    return handleChannel(uri)
//                }
//
//                "m" -> {
//                    return handleMarketing(uri)
//                }
//
//                // If the path looks like `/story/001078593`
//                "story" -> {
//                    val channelItem = ChannelItem(id = pathSegments[1], type = pathSegments[0], headline = "", shortlead = "")
//                    ContentActivity.start(activity, channelItem)
//                }
//
//                //
//                /**
//                 * If the path looks like `/tag/中美贸易战`,
//                 * start a new page listing articles
//                 */
//                "tag" -> {
//                    val page = ListPage(
//                            title = pathSegments[1],
//                            name = "${pathSegments[0]}_${pathSegments[1]}",
//                            listUrl = buildUrl(uri)
//                    )
//
//                    ChannelActivity.start(activity, page)
//                }
//
//                else -> {
//                    info("Open a content link directly. Original url is: $uri. API url is ${buildUrl(uri)}")
//                    ContentActivity.start(context, buildUrl(uri))
//                }
//            }
//
//            return true
//
//        }
//
//        private fun handleChannel(uri: Uri): Boolean {
//
//            val lastPathSegment = uri.lastPathSegment
//
//            if (lastPathSegment == null) {
//                val page = ListPage(
//                        title = "",
//                        name = "",
//                        listUrl = buildUrl(uri)
//                )
//
//                ChannelActivity.start(activity, page)
//                return true
//            }
//
//            when (lastPathSegment) {
//            /**
//             * If the path is `/channel/english.html`, navigate to the second bottom nav item.
//             */
//                "english.html" -> {
//                    navigateListener.selectBottomNavItem(R.id.nav_english)
//                }
//
//
//            /**
//             * If the path is `/channel/mba.html`, navigate to the third bottom nav item
//             */
//                "mba.html" -> {
//                    navigateListener.selectBottomNavItem(R.id.nav_ftacademy)
//                }
//
//
//            /**
//             * If the path is `/channel/weekly.html`
//             */
//                "weekly.html" -> {
//
//                    val tabIndex = ListPage.newsPages.indexOfFirst { it.name == "news_top_stories" }
//
//                    navigateListener.selectTabLayoutTab(tabIndex)
//                }
//
//                "markets.html" -> {
//                    val tabIndex = ListPage.newsPages.indexOfFirst { it.name == "news_markets" }
//
//                    navigateListener.selectTabLayoutTab(tabIndex)
//                }
//
//            /**
//             * Handle paths like:
//             * `/channel/editorchoice-issue.html?issue=EditorChoice-xxx`,
//             * `/channel/chinabusinesswatch.html`
//             * `/channel/viewtop.html`
//             * `/channel/teawithft.html`
//             * `/channel/markets.html`
//             * `/channel/money.html`
//             */
//                else -> {
//                    val issue = uri.getQueryParameter("issue")
//                    val name = issue ?: "channel_$lastPathSegment"
//
//                    val page = ListPage(
//                            title = pathToTitle[lastPathSegment] ?: "",
//                            name = name,
//                            listUrl = buildUrl(uri)
//                    )
//                    ChannelActivity.start(activity, page)
//                }
//            }
//
//            return true
//        }
//
//        private fun handleMarketing(uri: Uri): Boolean {
//            if (uri.pathSegments[1] == "marketing") {
//                when (uri.lastPathSegment) {
//
//                /**
//                 * If the path is `/m/marketing/intelligence.html`,
//                 * navigate to the tab titled FT研究院
//                 */
//                    "intelligence.html" -> {
//                        val tabIndex = ListPage.newsPages.indexOfFirst { it.name == "news_fta" }
//
//                        navigateListener.selectTabLayoutTab(tabIndex)
//                    }
//
//                /**
//                 * If the path looks like `/m/marketing/businesscase.html`
//                 */
//                    else -> {
//                        val name = uri.lastPathSegment ?: ""
//
//                        val page = ListPage(
//                                title = pathToTitle[name] ?: "",
//                                name = "marketing_$name",
//                                listUrl = buildUrl(uri)
//                        )
//                        ChannelActivity.start(activity, page)
//                    }
//                }
//
//                return true
//            }
//
//
//            /**
//             * There URLs looks like: `/m/corp/preview.html?pageid=we2016&isad=1`.
//             * Don't bother with them
//             */
//            val page = ListPage(
//                    title = "",
//                    name = "",
//                    listUrl = buildUrl(uri)
//            )
//            ChannelActivity.start(activity, page)
//
//            return true
//        }
//
//        private fun buildUrl(uri: Uri, path: String? = null): String {
//            val builder =  uri.buildUpon()
//                    .scheme("https")
//                    .authority("api003.ftmailbox.com")
//                    .appendQueryParameter("bodyonly", "yes")
//                    .appendQueryParameter("webview", "ftcapp")
//
//            if (path != null) {
//                builder.path(path)
//            }
//
//            return builder.build().toString()
//        }
//
//        /**
//         * Deal with advertisement. Open link in external browser. It seems Android does not have in-app browser
//         */
//        private fun handleExternalLink(uri: Uri): Boolean {
//            // This opens an external browser
//            val customTabsInt = CustomTabsIntent.Builder().build()
//            customTabsInt.launchUrl(context, uri)
//
//            return true
//        }
//    }

    inner class WebAppInterface : AnkoLogger {
        /**
         * Method injected to WebView to receive a list of articles in a currentPage page upon finished loading.
         */
        @JavascriptInterface
        fun postItems(message: String) {
            info("Posted items: $message")

            val channelContent = gson.fromJson<ChannelContent>(message, ChannelContent::class.java)

            channelItems = channelContent.sections[0].lists[0].items
            channelMeta = channelContent.meta
        }

        /**
         * See Page/Layouts/Page/SuperDataViewController.swift#SuperDataViewController what kind of data structure is passed back from web view.
         * The JSON data is parsed into SectionItem type in ContentActivity
         * iOS equivalent might be here: Page/Layouts/Pages/Content/DetailModelController.swift
         * @param index is the number of article user clicked in current page. The value is extracted from `data-row` attribute of `div.item-container-app`.
         * In most cases when you clicked, an article should be loaded.
         * In a few exceptions, like `/channel/column.html`, an item in the list should open another page of article list.
         * It seems the only way to distinguish those cases is using `ChannelMeta.title` field.
         */
        @JavascriptInterface
        fun selectItem(index: String) {
            info("Channel list click event: $index")

            Toast.makeText(activity, "Selected item $index", Toast.LENGTH_SHORT).show()


            if (channelMeta == null && channelItems == null) {
                return
            }

            val i = index.toInt()
            val channelItem = channelItems?.getOrNull(i) ?: return

            /**
             * For `column`, start a new ChannelActivity
             */
            when (channelMeta?.title) {
                "专栏" -> {
                    val listPage = ListPage(
                            title = channelItem.headline,
                            name = "${channelItem.type}_${channelItem.id}",
                            listUrl = buildUrl("/${channelItem.type}/${channelItem.id}")
                    )

                    ChannelActivity.start(context, listPage)
                    return
                }
            }


            /**
             * Now assuming this is a plain article
             */
            channelItem.adId = channelMeta?.adid ?: ""

            /**
             * Start different activity according to ChannelItem#type
             */
            StoryActivity.start(activity, channelItem)
        }


        private fun buildUrl(path: String): String {
            return Uri.Builder()
                .scheme("https")
                .authority("api003.ftmailbox.com")
                .path(path)
                .appendQueryParameter("bodyonly", "yes")
                .appendQueryParameter("webview", "ftcapp")
                .build()
                .toString()
        }
    }
}

class MyChromeClient : WebChromeClient(), AnkoLogger {

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        info("${consoleMessage?.lineNumber()} : ${consoleMessage?.message()}")
        return true
    }
}


