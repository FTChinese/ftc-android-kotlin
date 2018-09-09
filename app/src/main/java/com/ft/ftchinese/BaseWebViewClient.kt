package com.ft.ftchinese

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import android.support.customtabs.CustomTabsIntent
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.ft.ftchinese.models.ChannelItem
import com.ft.ftchinese.models.Endpoints
import com.ft.ftchinese.models.PagerTab
import com.ft.ftchinese.models.pathToTitle
import com.ft.ftchinese.user.Registration
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.warn

/**
 * A base WebViewClient that can be used directly or subclassed.
 * AbsContentActivity uses it directly.
 */
open class BaseWebViewClient(
        val activity: Activity?
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

    /**
     * Error code: -1, net::ERR_INCOMPLETE_CHUNKED_ENCODING
     */
    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        super.onReceivedError(view, request, error)

        info("onReceivedError: Failed to ${request?.method}: ${request?.url}")
        warn("Error code: ${error?.errorCode}, ${error?.description}")
    }

    override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
        super.onReceivedError(view, errorCode, description, failingUrl)
        info("onReceivedError: $errorCode, $description, $failingUrl")
    }

    // Handle clicks on a link in a web page loaded into url
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        info("shouldOverrideUrlLoading: $url")

        if (url == null) {
            return false
        }

        val uri = Uri.parse(url)

        // At the comment section of story page there is a login form.
        // Handle login in web view.
        // the login button calls js `bower_components/ftcnext/app/scripts/user-login-native.js`
        // It sends data:
        // username: String,
        // userId: String,
        // uniqueVisitorId: String,
        // paywall: String,
        // paywallExpire: String
        // The data structure is not compatible with our restful API. After we get this message in Java, it might be better send a request to API with `userId` so that native app always use API data.
        when (uri.scheme) {
            // The `免费注册` button is wrapped in a link with url set to `ftcregister://www.ftchinese.com/`
            "ftcregister" -> {
                Registration.startForResult(activity, REQUEST_CODE_SIGN_IN)
                return true
            }
            // The `微信登录` button is wrapped in a link with url set to `weixinlogin://www.ftchinese.com/`
            /**
             * @TODO Call wechat
             */
            "weixinlogin" -> {
                info("Request wechat login")
                return true
            }
        }
        /**
         * If the clicked url is of the pattern `.../story/xxxxxx`, you should use `StoryActivity`
         * and fetch JSON from server and concatenate it with a html bundle into the package `raw/story.html`,
         * then call `WebView.loadDataWithBaseUrl()` to load the string into WebView.
         * In such a case, you need to provide a base url so that contents in the WebView know where to fetch resources (like advertisement).
         * The base url for such contents should be `www.ftchinese.com`.
         * If you load a url directly into, the host might be something else, like `api003.ftmailbox.com`, depending your url you use.
         * Here we check origin or the clicked URL: for "www.ftchinese.com" or "api003.ftmailbox.com", we load them directly into the app.
         * For external links (mostly ads), open in external browser.
         */
        if (Endpoints.hosts.contains(uri.host)) {
            return handleInSiteLink(uri)
        }

        return handleExternalLink(uri)
    }

    private fun handleInSiteLink(uri: Uri): Boolean {
        val pathSegments = uri.pathSegments


        /**
         * Determine if the link is a channel's pagination link.
         * `page` query parameter is used for each channel's pagination.
         * For unknown reasons, the pagination url uses relative links, you have to handle it separately.
         */
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

            /**
             * Loads an article that do not have JSON api and load it directly into the WebView.
             * WebContentActivity accepts a String url.
             * Example:
             * `FT研究院` is a web page loaded directly into WebView. You could only click a title's link to read this an article.
             * The link is actually pointing to somewhere like `http://www.ftchinese.com/interactive/12376`.
             * But you want it to load a stripped HTML web page on `https://api003.ftmailbox.com/interactive/12376?bodyonly=no&webview=ftcapp&i=3&0=01&exclusive`.
             * To to this, you have to transform the URL using `buildUrl`.
             */

            else -> {
                info("Open a web page directly. Original url is: $uri")
                WebContentActivity.start(activity, uri)

                true
            }
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
     * A list of articles
     */
    open fun openChannelLink(uri: Uri): Boolean {

        val lastPathSegment = uri.lastPathSegment

        val page = PagerTab(
                title = pathToTitle[lastPathSegment] ?: "",
                name = "",
                fragmentUrl = buildUrl(uri))

        ChannelActivity.start(activity, page)

        return true
    }

    /**
     * This kind of page is a list of articles
     */
    open fun openMarketingLink(uri: Uri): Boolean {
        val lastPathSegment = uri.lastPathSegment

        val page = PagerTab(
                title = pathToTitle[lastPathSegment] ?: "",
                name = "",
                fragmentUrl = buildUrl(uri))

        ChannelActivity.start(activity, page)

        return true
    }

    /**
     * Loads a story page who has JSON api on server
     * StoryActivity accepts a ChannelItem parameter.
     */
    private fun openStoryLink(uri: Uri): Boolean {
        val channelItem = ChannelItem(
                id = uri.pathSegments[1],
                type = uri.pathSegments[0],
                headline = "",
                shortlead = "")
        StoryActivity.start(activity, channelItem)

        return true
    }

    private fun handleExternalLink(uri: Uri): Boolean {
        // This opens an external browser
        val customTabsInt = CustomTabsIntent.Builder().build()
        customTabsInt.launchUrl(activity, uri)

        return true
    }

    private fun openTagLink(uri: Uri): Boolean {
        val page = PagerTab(
                title = uri.pathSegments[1],
                name = "${uri.pathSegments[0]}_${uri.pathSegments[1]}",
                fragmentUrl = buildUrl(uri))

        ChannelActivity.start(activity, page)

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