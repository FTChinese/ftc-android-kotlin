package com.ft.ftchinese

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.support.v4.view.MenuItemCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.ShareActionProvider
import android.text.Html
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.*
import android.widget.Toast

import kotlinx.android.synthetic.main.activity_content.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.warn

/**
 * This is used to show the content of an article.
 */
class ContentActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener, AnkoLogger {

    private var channelItem: ChannelItem? = null
    private var cacheFilename: String? = null
    private var isLoadUrl: Boolean = false
    private var shareActionProvider: ShareActionProvider? = null

    companion object {
        private const val EXTRA_CHANNEL_ITEM = "extra_channel_item"
        private const val EXTRA_LOAD_URL = "extra_load_url"

        fun start(context: Context?, channelItem: ChannelItem) {
            val intent = Intent(context, ContentActivity::class.java)
            intent.putExtra(EXTRA_CHANNEL_ITEM, gson.toJson(channelItem))
            context?.startActivity(intent)
        }

        fun start(context: Context?, url: String) {
            val intent = Intent(context, ContentActivity::class.java)
            intent.putExtra(EXTRA_LOAD_URL, url)
            context?.startActivity(intent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        info("Create options menu")
        menuInflater.inflate(R.menu.share, menu)

        /**
         * Warning: On its doc (https://developer.android.com/reference/android/support/v4/view/ActionProvider), they say:
         *
         * > If you're developing your app for API level 14 and higher only, you should instead use the framework ActionProvider class
         *
         * This expression is misleading. As long as you are using `AppCompatActivity`, you could only use the support library's version.
         */
        val shareItem = menu.findItem(R.id.action_share)
        /**
         * `MenuItem` has a similar method `getActionProvider()`. It used for the platform's version of `ShareActionProvider`.
         * For support library you must use `MenuItemCompat`'s static method.
         */
        shareActionProvider = MenuItemCompat.getActionProvider(shareItem) as ShareActionProvider

        val intent = Intent(Intent.ACTION_SEND, Uri.parse("http://www.ftchinese.com/story/${channelItem?.id}"))
        intent.type = "text/plain"
//        intent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml("<p>This is the text shared.</p>"))
        intent.putExtra(Intent.EXTRA_TEXT, "[FT中文网] ${channelItem?.headline} - http://www.ftchinese.com/story/${channelItem?.id}")

        setShareIntent(intent)


        return super.onCreateOptionsMenu(menu)
    }

    private fun setShareIntent(shareIntent: Intent) {

        shareActionProvider?.setShareIntent(shareIntent)
    }

    // Handle app bar items selected
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        info("Options item selected: ${item?.itemId}")

        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_content)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }

        swipe_refresh.setOnRefreshListener(this)

        web_view.settings.apply {
            javaScriptEnabled = true
            loadsImagesAutomatically = true
        }

        web_view.apply {

            addJavascriptInterface(WebAppInterface(), "Android")

            webViewClient = ContentWebViewClient()
            webChromeClient = MyChromeClient()

            setOnKeyListener { v, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && web_view.canGoBack()) {
                    web_view.goBack()
                    return@setOnKeyListener true
                }

                false
            }
        }

        // Load a URL directly into web view
        val url = intent.getStringExtra(EXTRA_LOAD_URL)

        if (url != null) {
            isLoadUrl = true
            web_view.loadUrl(url)
            stopProgress()
            return
        }

        // If JS intercepted a click event in WebView and passed back data, parse the data to SectionItem
        val extraContent = intent.getStringExtra(EXTRA_CHANNEL_ITEM)

        // It contain the information to retrieve an article
        channelItem = gson.fromJson(extraContent, ChannelItem::class.java)
        cacheFilename = "${channelItem?.type}_${channelItem?.id}.html"

        // Start retrieving data from cache or server
        init()

    }

    override fun onRefresh() {
        Toast.makeText(this, "Refreshing", Toast.LENGTH_SHORT).show()

        // If the page is directly loaded with url, call WebView's reload method.
        if (isLoadUrl) {
            web_view.reload()
            stopProgress()
            return
        }

        // Otherwise use WebView.loadDataWithBaseUrl
        launch(UI) {
            fetchAndUpdate()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        info("Activity destroyed")
    }

    private fun init() {

        showProgress()

        launch(UI) {

            // Load from cache
            val readResult = async { Store.load(this@ContentActivity, cacheFilename) }
            val cachedData = readResult.await()

            if (cachedData != null) {
                Toast.makeText(this@ContentActivity, "Using cache", Toast.LENGTH_SHORT).show()
                updateUi(cachedData)

                return@launch
            }

            // Cache is not found.
            fetchAndUpdate()
        }
    }

    private suspend fun fetchAndUpdate() {
        when (channelItem?.type) {
            "story", "premium" -> fetchJSON()
            else -> loadUrl()
        }
    }

    // Use local HTML template and remote JSON to generate a complete HTML file
    private suspend fun fetchJSON() {
        // Fetch data from server
        val url = channelItem?.apiUrl ?: return

        val readResult = async { readHtml(resources, R.raw.story) }
        val fetchResult = async { requestData(url) }

        val template = readResult.await()
        val jsonData = fetchResult.await()

        // Cannot fetch data from server
        if (jsonData == null || template == null) {
            Toast.makeText(this@ContentActivity, "Error! Failed to load data", Toast.LENGTH_SHORT).show()
            stopProgress()
            return
        }

        val data = renderTemplate(template, jsonData)

        updateUi(data)

        async { Store.save(this@ContentActivity, cacheFilename, data) }
    }


    /**
     * See: Page/Helpers/WebView/WebViewHelper.swift#renderStory
     */
    private fun renderTemplate(template: String, data: String): String {
        val article = gson.fromJson<ArticleDetail>(data, ArticleDetail::class.java)

        return template.replace("{story-body}", article.bodyXML.cn)
                .replace("{story-headline}", article.titleCn)
                .replace("{story-byline}", article.byline)
                .replace("{story-time}", article.createdAt)
                .replace("{story-lead}", article.standfirst)
                .replace("{story-theme}", article.htmlForTheme())
                .replace("{story-tag}", article.tag)
                .replace("{story-id}", article.id)
                .replace("{story-image}", article.htmlForCoverImage())
                .replace("{related-stories}", article.htmlForRelatedStories())
                .replace("{related-topics}", article.htmlForRelatedTopics())
                //                        .replace("{comments-order}", "")
                //                        .replace("{story-container-style}", "")
                //                        .replace("['{follow-tags}']", "")
                //                        .replace("['{follow-topics}']", "")
                //                        .replace("['{follow-industries}']", "")
                //                        .replace("['{follow-areas}']", "")
                //                        .replace("['{follow-authors}']", "")
                //                        .replace("['{follow-columns}']", "")
                .replace("{adchID}", channelItem?.adId!!)
                //                        .replace("{ad-banner}", "")
                //                        .replace("{ad-mpu}", "")
                //                        .replace("{font-class}", "")
                .replace("{comments-id}", channelItem?.commentsId!!)
    }

    private fun updateUi(data: String) {

        web_view.loadDataWithBaseURL("http://www.ftchinese.com", data, "text/html", null, null)

        stopProgress()
    }

    // Directly load a url
    private fun loadUrl() {
        val url = channelItem?.apiUrl ?: return

        info("loadUrl: $url")

        web_view.loadUrl(url)
        stopProgress()
    }

    private fun showProgress() {
        progress_bar.visibility = View.VISIBLE
    }

    private fun stopProgress() {
        swipe_refresh.isRefreshing = false
        progress_bar.visibility = View.GONE
    }


    inner class ContentWebViewClient : WebViewClient(), AnkoLogger {


        override fun onLoadResource(view: WebView?, url: String?) {
            super.onLoadResource(view, url)
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)

        }


        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
            super.onReceivedError(view, request, error)

            info("Failed to ${request?.method}: ${request?.url}")
            warn(error.toString())
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
            val pathSegments = uri.pathSegments

            if (pathSegments.size >= 2 && pathSegments[0] == "story") {
                val channelItem = ChannelItem(id = pathSegments[1], type = pathSegments[0], headline = "", shortlead = "")
                ContentActivity.start(this@ContentActivity, channelItem)
                return true
            }

            val newUrl = uri.buildUpon()
                    .scheme("https")
                    .authority("api003.ftmailbox.com")
                    .appendQueryParameter("bodyonly", "yes")
                    .appendQueryParameter("webview", "ftcapp")
                    .build()
                    .toString()
            ContentActivity.start(this@ContentActivity, newUrl)
            return true
        }

        private fun handleExternalLink(uri: Uri): Boolean {
            // This opens an external browser
            val customTabsInt = CustomTabsIntent.Builder().build()
            customTabsInt.launchUrl(this@ContentActivity, uri)

            return true
        }
    }

    inner class WebAppInterface {

        @JavascriptInterface
        fun postMessage(message: String) {
            Log.i("WebChromeClient", "Click event: $message")

            val intent = Intent(this@ContentActivity, ContentActivity::class.java)
            intent.putExtra(EXTRA_SECTION_ITEM, message)
            startActivity(intent)
        }
    }
}
