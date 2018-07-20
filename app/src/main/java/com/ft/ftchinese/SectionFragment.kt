package com.ft.ftchinese

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.util.Log
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

const val EXTRA_SECTION_ITEM = "section_item"
const val EXTRA_DIRECT_OPEN = "article_direct_open"
const val EXTRA_CHANNEL_ITEM = "channel_item"
const val EXTRA_CHANNEL_AD_ID = "channel_ad_id"

class SectionFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener, AnkoLogger {

    private val TAG = "SectionFragment"
    private lateinit var listener: OnDataLoadListener

    // Specify what kind of data to use for current tab, retrieved from fragment arguments.
    private var channel: Channel? = null

    // iOS equivalent might be defined Page/Layouts/Pages/Content/DetailModelController.swift#pageData
    private var channelItems: Array<ChannelItem>? = null
    private var adId: String? = null

    // Containing activity should implement this interface to show progress state
    interface OnDataLoadListener {
        fun onDataLoaded()

        fun onDataLoading()
    }

    companion object {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private val ARG_SECTION_CHANNEL = "section_chanel"
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
        fun newInstance(channel: String): SectionFragment {
            val fragment = SectionFragment()
            val args = Bundle()
            args.putString(ARG_SECTION_CHANNEL, channel)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onAttach(context: Context?) {
        Log.i(TAG, "onAttach fragment")
        super.onAttach(context)
        listener = context as OnDataLoadListener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val channelData = arguments?.getString(ARG_SECTION_CHANNEL)

        channel = gson.fromJson<Channel>(channelData, Channel::class.java)
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
            webViewClient = SectionWebViewClient()
            webChromeClient = MyChromeClient()
        }

        web_view.setOnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && web_view.canGoBack()) {
                web_view.goBack()
                return@setOnKeyListener true
            }

            false
        }

        Log.i(TAG, "Initiating data...")

        init()

        Log.i(TAG, "Finish onCreateView")
    }


    override fun onRefresh() {
        Toast.makeText(context, "Refreshing", Toast.LENGTH_SHORT).show()

        launch(UI) {
            fetchAndUpdate()
        }
    }

    private fun init() {

        showProgress()

        if (channel?.listUrl != null ) {

            launch (UI) {
                if (channel?.name != null) {
                    val readCacheResult = async { Store.load(context, "${channel?.name}.html") }
                    val cachedHtml = readCacheResult.await()

                    if (cachedHtml != null) {
                        Log.i(TAG, "Using cached data for ${channel?.name}")

                        updateUi(cachedHtml)

                        return@launch
                    }
                }

                fetchAndUpdate()
            }

            return
        }

        if (channel?.webUrl != null) {
            web_view.loadUrl(channel?.webUrl)

            stopProgress()
        }

    }

    private suspend fun fetchAndUpdate() {
        val readResult = async { readHtml(resources, R.raw.list) }
        info("Fetch channel data ${channel?.listUrl}")

        val fetchResult = async { requestData(channel?.listUrl!!) }

        val templateHtml = readResult.await()
        val remoteHtml = fetchResult.await()

        if (templateHtml == null || remoteHtml == null) {
            updateUi(HTML_PLACEHOLDER)
            return
        }


        val htmlString = templateHtml.replace("{list-content}", remoteHtml)

        updateUi(htmlString)

        // Cache file

        if (channel?.name != null) {
            async { Store.save(context, "${channel?.name}.html", htmlString) }
        }
    }

    private fun updateUi(data: String) {
        web_view.loadDataWithBaseURL("http://www.ftchinese.com", data, "text/html", null, null)
        stopProgress()
    }

    private fun showProgress() {
        listener.onDataLoading()
    }

    private fun stopProgress() {
        listener.onDataLoaded()
        swipe_refresh_layout.isRefreshing = false
    }

    inner class SectionWebViewClient : WebViewClient(), AnkoLogger {

        override fun onLoadResource(view: WebView?, url: String?) {
            super.onLoadResource(view, url)
//            info("Will loading resource $url")
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
//            info("Page started loading: $url")
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
//            info("Page finished loading: $url")
        }

        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
            super.onReceivedError(view, request, error)
            warn("Failed to ${request?.method}: ${request?.url}. error.toString()")
        }

        override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
            super.onReceivedHttpError(view, request, errorResponse)

            warn("HTTP error - ${request?.method}: ${request?.url}")
        }

        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            info("shouldOverrideUrlLoading: $url")

            if (url == null) {
                return false
            }
            val uri = Uri.parse(url)

            if (uri.host == "www.ftchinese.com") {

                return handleInSiteLink(uri)
            }

            return handleExternalLink(uri)
        }

        private fun handleInSiteLink(uri: Uri): Boolean {
            val newUrl = uri.buildUpon()
                    .scheme("https")
                    .authority("api003.ftmailbox.com")
                    .appendQueryParameter("bodyonly", "yes")
                    .appendQueryParameter("webview", "ftcapp")
                    .build()
                    .toString()
            when (uri.lastPathSegment) {
                "editorchoice-issue.html" -> {
                    info("Clicked editor's choice channel. Will load data from $newUrl")

                    val channel = Channel(
                            title = "编辑精选",
                            name = "news_editor_choice_channel",
                            listUrl = newUrl
                    )
                    ChannelActivity.start(activity, channel)
                }
                "english.html" -> {
                    info("Clicked english channel. Will fetch data from ${newUrl}")
                    val channel = Channel(
                            title = "每日英语",
                            name = "english_channel",
                            listUrl = newUrl
                    )
                    ChannelActivity.start(activity, channel)
                }
                "mba.html" -> {
                    info("Clicked ft academy channel. Will fetch data from $newUrl")
                    val channel = Channel(
                            title = "FT商学院",
                            name = "fta_channel",
                            listUrl = newUrl
                    )
                    ChannelActivity.start(activity, channel)
                }

                "intelligence.html" -> {
                    info("Clicked ft intelligence channel. Will fetch data from $newUrl")
                    val channel = Channel(
                            title = "FT研究院",
                            name = "fti_channel",
                            listUrl = newUrl
                    )
                    ChannelActivity.start(activity, channel)
                }

                "businesscase.html" -> {
                    info("Clicked business case. Will fetch data from $newUrl")
                    val channel = Channel(
                            title = "中国商业案例精选",
                            name = "business_case",
                            listUrl = newUrl
                    )
                    ChannelActivity.start(activity, channel)
                }

                else -> {
                    info("Assuming a link is clicked. Open directly $newUrl")
                    ContentActivity.start(context, newUrl)
                }
            }

            return true
        }

        private fun handleExternalLink(uri: Uri): Boolean {
            // This opens an external browser
            val customTabsInt = CustomTabsIntent.Builder().build()
            customTabsInt.launchUrl(context, uri)

            return true
        }
    }

    inner class WebAppInterface : AnkoLogger {
        @JavascriptInterface
        fun postItems(message: String) {
            info("Posted items: $message")

            val channelContent = gson.fromJson<ChannelContent>(message, ChannelContent::class.java)

            channelItems = channelContent.sections[0].lists[0].items
            adId = channelContent.meta.adid
        }

        // See Page/Layouts/Page/SuperDataViewController.swift#SuperDataViewController what kind of data structure is passed back from web view.
        // The JSON data is parsed into SectionItem type in ContentActivity
        // iOS equiavalent might be here: Page/Layouts/Pages/Content/DetailModelController.swift
        @JavascriptInterface
        fun selectItem(index: String) {
            info("WebView click event: $index")

            Toast.makeText(activity, "Selected item $index", Toast.LENGTH_SHORT).show()

            val i = index.toInt()

            if (channelItems == null) {
                return
            }

            val channelItem = channelItems?.getOrNull(i) ?: return

            channelItem.adId = adId ?: ""

            ContentActivity.start(activity, channelItem)
        }

    }
}

class MyChromeClient : WebChromeClient(), AnkoLogger {

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        info("${consoleMessage?.lineNumber()} : ${consoleMessage?.message()}")
        return true
    }
}


