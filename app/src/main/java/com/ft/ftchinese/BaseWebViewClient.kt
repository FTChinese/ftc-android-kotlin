package com.ft.ftchinese

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.support.customtabs.CustomTabsIntent
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.warn

/**
 * A base WebViewClient that can be used directly or subclassed.
 * AbstractContentActivity uses it directly.
 */
open class BaseWebViewClient(
        private val context: Context?
) : WebViewClient(), AnkoLogger {
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

        /**
         * If you load content using `StoryActivity`, the base url in web view is `www.ftchinese.com`.
         * If you load a url directly into, the host might be something else, like `api003.ftmailbox.com`, depending your url you use.
         */
        if (Endpoints.hosts.contains(uri.host)) {
            return handleInSiteLink(uri)
        }

        return handleExternalLink(uri)
    }

    private fun handleInSiteLink(uri: Uri): Boolean {
        val pathSegments = uri.pathSegments


        if (pathSegments.size < 2 && uri.getQueryParameter("page") != null) {
            return openChannelPagination(uri)
        }

        /**
         * Path segments have two or more parts
         */
        return when (pathSegments[0]) {

        /**
         * If the path looks like `/channel/english.html`
         */
            "channel" -> openChannelLink(uri)

        /**
         * If the path looks like `/m/marketing/intelligence.html`
         */
            "m" -> openMarketingLink(uri)

        /**
         * If the path looks like `/story/001078593`
         */
            "story", "premium" -> openStoryLink(uri)


        /**
         * If the path looks like `/tag/中美贸易战`,
         * start a new page listing articles
         */
            "tag" -> openTagLink(uri)

            else -> openWebContent(uri)
        }
    }

    /**
     * Handle the pagination link of each channel
     * There's a problem with each channel's pagination: they used relative urls.
     * When loaded in WebView with base url `http://www.ftchinese.com`,
     * the url will become something `http://www.ftchinese.com/china.html?page=2`,
     * which should actually be `http://www.ftchiese.com/channel/china.html?page=2`
     */
    open fun openChannelPagination(uri: Uri): Boolean {
        return false
    }

    /**
     * Placeholder implementation
     */
    open fun openChannelLink(uri: Uri): Boolean {

        val lastPathSegment = uri.lastPathSegment

        val page = ListPage(
                title = pathToTitle[lastPathSegment] ?: "",
                name = "",
                listUrl = buildUrl(uri))

        ChannelActivity.start(context, page)

        return true
    }

    /**
     * Placeholder implementation
     */
    open fun openMarketingLink(uri: Uri): Boolean {
        val lastPathSegment = uri.lastPathSegment

        val page = ListPage(
                title = pathToTitle[lastPathSegment] ?: "",
                name = "",
                listUrl = buildUrl(uri))

        ChannelActivity.start(context, page)

        return true
    }

    private fun openStoryLink(uri: Uri): Boolean {
        val channelItem = ChannelItem(
                id = uri.pathSegments[1],
                type = uri.pathSegments[0],
                headline = "",
                shortlead = "")
        StoryActivity.start(context, channelItem)

        return true
    }

    private fun openTagLink(uri: Uri): Boolean {
        val page = ListPage(
                title = uri.pathSegments[1],
                name = "${uri.pathSegments[0]}_${uri.pathSegments[1]}",
                listUrl = buildUrl(uri))

        ChannelActivity.start(context, page)

        return true
    }

    private fun openWebContent(uri: Uri): Boolean {
        info("Open a web page directly. Original url is: $uri. API url is ${buildUrl(uri)}")
        WebContentActivity.start(context, buildUrl(uri))

        return true
    }

    private fun handleExternalLink(uri: Uri): Boolean {
        // This opens an external browser
        val customTabsInt = CustomTabsIntent.Builder().build()
        customTabsInt.launchUrl(context, uri)

        return true
    }

    fun buildUrl(uri: Uri, path: String? = null): String {
        val builder =  uri.buildUpon()
                .scheme("https")
                .authority("api003.ftmailbox.com")
                .appendQueryParameter("bodyonly", "yes")
                .appendQueryParameter("webview", "ftcapp")

        if (path != null) {
            builder.path(path)
        }

        return builder.build().toString()
    }
}