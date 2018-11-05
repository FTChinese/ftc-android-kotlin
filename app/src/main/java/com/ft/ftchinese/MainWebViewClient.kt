package com.ft.ftchinese

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.support.customtabs.CustomTabsIntent
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.ft.ftchinese.models.*
import com.ft.ftchinese.user.SignUpActivity
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

/**
 * A base WebViewClient that can be used directly or subclassed.
 * AbsContentActivity uses it directly.
 * This is used to handle various event when loading a web page,
 * particularly the click event on a url.
 */
open class MainWebViewClient(
        val activity: Activity?
) : WebViewClient(), AnkoLogger {

    /**
     * Callback used by ChannelWebViewClient.
     * When certain links in web view is clicked, the event is passed to parent activity to open a bottom navigation item or a tab.
     */
    private var mListener: OnInAppNavigateListener? = null

    private var mPaginateListener: OnPaginateListener? = null
    /**
     * Jump to another bottom navigation item or another tab in hte same bottom navigation item when certain links are clicked.
     */
    interface OnInAppNavigateListener {
        // Jump to a new bottom navigation item
        fun onGotoBottomNavItem(itemId: Int)

        // Go to another tab
        fun onGotoTabLayoutTab(tabIndex: Int)
    }

    interface OnPaginateListener {
        fun onPagination(page: String)
    }

    fun setOnInAppNavigateListener(listener: OnInAppNavigateListener?) {
        mListener = listener
    }

    fun setOnPaginateListener(listener: OnPaginateListener?) {
        mPaginateListener = listener
    }

    // Handle clicks on a link in a web page loaded into url
    // Returns true if you handled url links yourself;
    // returns false Android will try to handle it.
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
        return when (uri.scheme) {
            // 通过邮件反馈 link: mailto:ftchinese.feedback@gmail.com?subject=Feedback
            "mailto" -> {
                return feedbackEmail()
            }
            // The `免费注册` button is wrapped in a link with url set to `ftcregister://www.ftchinese.com/`
            "ftcregister" -> {
                SignUpActivity.start(activity)
                return true
            }
            // The `微信登录` button is wrapped in a link with url set to `weixinlogin://www.ftchinese.com/`
            "weixinlogin" -> {
                info("Request wechat login")
                return true
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
            "http", "https" -> {
                if (Endpoints.hosts.contains(uri.host)) {
                    return handleInSiteLink(uri)
                }

                return handleExternalLink(uri)
            }
            else -> false
        }
    }

    private fun feedbackEmail(): Boolean {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, "ftchinese.feedback@gmail.com")
            putExtra(Intent.EXTRA_SUBJECT, "Feedback on FTC Android App")
        }

        return if (intent.resolveActivity(activity?.packageManager) != null) {
            activity?.startActivity(intent)
            true
        } else {
            activity?.toast(R.string.prompt_no_email_app)
            true
        }
    }

    private fun handleInSiteLink(uri: Uri): Boolean {
        val pathSegments = uri.pathSegments

        info("Handle in-site link $uri")
        /**
         * Determine if the link is a channel's pagination link.
         * `page` query parameter is used for each channel's pagination.
         * For unknown reasons, the pagination url uses relative links, you have to handle it separately.
         */
        if (uri.getQueryParameter("page") != null || uri.getQueryParameter("p") != null) {
            info("Open channel pagination for uri: $uri")
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
            ChannelItem.TYPE_STORY, ChannelItem.TYPE_PREMIUM -> openArticleLink(uri)

            ChannelItem.TYPE_VIDEO -> openVideoLink(uri)

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
     * Loads a story page who has JSON api on server
     * StoryActivity accepts a ChannelItem parameter.
     */
    private fun openArticleLink(uri: Uri): Boolean {
        val channelItem = ChannelItem(
                id = uri.pathSegments[1],
                type = uri.pathSegments[0],
                headline = "",
                shortlead = "")
        StoryActivity.start(activity, channelItem)

        return true
    }

    private fun openVideoLink(uri: Uri): Boolean {
        val channelItem = ChannelItem(
                id = uri.pathSegments[1],
                type = uri.pathSegments[0],
                headline = "",
                shortlead = "")

        WebContentActivity.start(activity, channelItem)

        return true
    }
    /**
     * Handle the pagination link of each channel
     * There's a problem with each channel's pagination: they used relative urls.
     * When loaded in WebView with base url `http://www.ftchinese.com`,
     * the url will become something `http://www.ftchinese.com/china.html?page=2`,
     * which should actually be `http://www.ftchiese.com/channel/china.html?page=2`
     *
     * However,
     * `columns` uses /column/007000049?page=2
     * English radio uses http://www.ftchinese.com/channel/radio.html?p=2
     * Speed read uses http://www.ftchinese.com/channel/speedread.html?p=2
     * Bilingual reading uses http://www.ftchinese.com/channel/ce.html?p=2
     *
     */
    open fun openChannelPagination(uri: Uri): Boolean {


        var pageKey: String? = null
        var pageNumber: String? = null

        pageNumber = uri.getQueryParameter("page")
        if (pageNumber != null) {
            pageKey = "page"
        } else {
            pageNumber = uri.getQueryParameter("p")

            if (pageNumber != null) {
                pageKey = "p"
            }
        }

        if (pageKey == null || pageNumber == null) { return true }

        val key = uri.lastPathSegment ?: return true

        val pageMeta = paginationMap[key] ?: return true
        val url = Uri.parse(pageMeta.contentUrl)
                .buildUpon()
                .appendQueryParameter(pageKey, pageNumber)
                .build()
                .toString()

        val listPage = PagerTab(
                title = pageMeta.title,
                name = "${pageMeta.name}_$pageNumber",
                contentUrl = url,
                htmlType = pageMeta.htmlType
        )

        info("Open channel page ${listPage.contentUrl}")

        ChannelActivity.start(activity, listPage)

        return true
    }

    /**
     * A list of articles
     */
    open fun openChannelLink(uri: Uri): Boolean {

        val lastPathSegment = uri.lastPathSegment

        val page = PagerTab(
                title = pathToTitle[lastPathSegment] ?: "",
                name = uri.pathSegments.joinToString("_").removeSuffix(".html"),
                contentUrl = buildUrl(uri),
                htmlType = PagerTab.HTML_TYPE_FRAGMENT
        )

        ChannelActivity.start(activity, page)

        return true
    }

    /**
     * This kind of page is a list of articles
     */
    open fun openMarketingLink(uri: Uri): Boolean {
        info("Open a marketing link: $uri")
        when (uri.pathSegments[1]) {
            "corp" -> {
                val listPage = PagerTab(
                        title = "",
                        name = "",
                        contentUrl = uri.buildUpon()
                                .appendQueryParameter("webview", "ftcapp")
                                .build()
                                .toString(),
                        htmlType = PagerTab.HTML_TYPE_COMPLETE
                )

                ChannelActivity.start(activity, listPage)
                return true
            }
            "marketing" -> {
                when (uri.lastPathSegment) {

                    /**
                     * If the path is `/m/marketing/intelligence.html`,
                     * navigate to the tab titled FT研究院
                     */
                    "intelligence.html" -> {
                        val tabIndex = Navigation.newsPages.indexOfFirst { it.name == "news_fta" }

                        mListener?.onGotoTabLayoutTab(tabIndex)
                    }
                    /**
                     * If the path looks like `/m/marketing/businesscase.html`
                     */
                    else -> {
                        val name = uri.lastPathSegment ?: ""
                        val listPage = PagerTab(
                                title = pathToTitle[name] ?: "",
                                name = "marketing_$name",
                                contentUrl = uri.buildUpon().appendQueryParameter("webview", "ftcapp").build().toString(),
                                htmlType = PagerTab.HTML_TYPE_COMPLETE
                        )

                        ChannelActivity.start(activity, listPage)
                    }
                }

                return true
            }
        }

        val lastPathSegment = uri.lastPathSegment

        val listPage = PagerTab(
                title = pathToTitle[lastPathSegment] ?: "",
                name = "",
                contentUrl = buildUrl(uri),
                htmlType = PagerTab.HTML_TYPE_COMPLETE
        )

        ChannelActivity.start(activity, listPage)

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
                title = uri.lastPathSegment,
                name = "${uri.pathSegments[0]}_${uri.pathSegments[1]}",
                contentUrl = buildUrl(uri),
                htmlType = PagerTab.HTML_TYPE_FRAGMENT
        )

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

val paginationMap = mapOf(
        "china.html" to Navigation.newsPages[1],
        "world.html" to Navigation.newsPages[4],
        "opinion.html" to Navigation.newsPages[5],
        "markets.html" to Navigation.newsPages[7],
        "business.html" to Navigation.newsPages[8],
        "management.html" to Navigation.newsPages[11],
        "lifestyle.html" to Navigation.newsPages[12],
        "radio.html" to Navigation.englishPages[0],
        "speedread.html" to Navigation.englishPages[1],
        "ce.html" to Navigation.englishPages[2]
)