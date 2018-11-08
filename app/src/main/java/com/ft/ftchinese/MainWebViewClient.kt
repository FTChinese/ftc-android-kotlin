package com.ft.ftchinese

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.support.customtabs.CustomTabsIntent
import android.webkit.WebView
import android.webkit.WebViewClient
import com.ft.ftchinese.models.*
import com.ft.ftchinese.user.SignUpActivity
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

/**
 * Those links need to start a ChannelActivity.
 * Other links include:
 * /tag/汽车未来
 */
val pathToTitle = mapOf(
        // /channel/english.html?webview=ftcapp
        "english.html" to "每日英语",
        // /channel/mba.html?webview=ftcapp
        "mba.html" to "FT商学院",
        // /m/marketing/intelligence.html?webview=ftcapp
        "intelligence.html" to "FT研究院",
        // /m/marketing/businesscase.html
        "businesscase.html" to "中国商业案例精选",
        // /channel/weekly.html
        "weekly.html" to "热门文章",
        // /channel/editorchoice-issue.html?issue=EditorChoice-20181029
        "editorchoice-issue.html" to "编辑精选",
        // /channel/chinabusinesswatch.html
        "chinabusinesswatch.html" to "宝珀·中国商业观察",
        // /m/corp/preview.html?pageid=huawei2018
        "huawei2018" to "+智能 见未来 重塑商业力量",
        // /channel/tradewar.html
        "tradewar.html" to "中美贸易战",
        "viewtop.html" to "高端视点",
        "Emotech2017.html" to "2018·预见人工智能",
        "antfinancial.html" to "“新四大发明”背后的中国浪潮",
        "teawithft.html" to "与FT共进下午茶",
        "creditease.html" to "未来生活 未来金融",
        "markets.html" to "金融市场",
        "hxxf2016.html" to "透视中国PPP模式",
        "money.html" to "理财"
)

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

/**
 * MainWebViewClient is use mostly to handle url clicks loaded into
 * ViewPagerFragment.
 */
open class MainWebViewClient(
        val activity: Activity?
) : WebViewClient(), AnkoLogger {

    // Pass click events to host.
    private var mListener: OnClickListener? = null

    interface OnClickListener {
        fun onClickUrl()
    }

    fun setOnClickListener(listener: OnClickListener?) {
        mListener = listener
    }
    // Handle clicks on a link in a web page loaded into url
    // Returns true if you handled url links yourself;
    // returns false Android will try to handle it.
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        info("shouldOverrideUrlLoading: $url")

        // If url is null, do nothing.
        if (url == null) {
            return true
        }

        val uri = Uri.parse(url)

        // Tell host that a url is clicked.
        // Host should handle the event.
        mListener?.onClickUrl()


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
            else -> true
        }
    }



    private fun handleInSiteLink(uri: Uri): Boolean {
        val pathSegments = uri.pathSegments

        info("Handle in-site link $uri")
        /**
         * Handle pagination links.
         * This will start a new ChannelActivity.
         */
        if (uri.getQueryParameter("page") != null || uri.getQueryParameter("p") != null) {
            info("Open channel pagination for uri: $uri")
            return openChannelPagination(uri)
        }

        /**
         * URL needs to be handled on home page
         * 每日英语 /channel/english.html?webview=ftcapp
         * FT商学院 /channel/mba.html?webview=ftcapp
         * FT商学院 articles /photonews/1082
         * FT研究院 /m/marketing/intelligence.html?webview=ftcapp
         * FT研究院 articles /interactive/12781
         * 热门文章 /channel/weekly.html
         * 热门文章 articles /story/xxxx
         *
         * It plays similar roles like JS interface `onSelectItem`
         * but might differs.
         *
         * We assume pathSegments[0] plans similar roles as
         * ChannelItem.type.
         */
        return when (pathSegments[0]) {

            /**
             * Handle various article-like urls first.
             * If the path looks like `/story/001078593`
             */
            ChannelItem.TYPE_STORY,
            ChannelItem.TYPE_PREMIUM,
            ChannelItem.TYPE_VIDEO,
            ChannelItem.TYPE_INTERACTIVE,
            ChannelItem.TYPE_PHOTONEWS -> {
                val channelItem = ChannelItem(
                        id = pathSegments[1],
                        type = pathSegments[0],
                        headline = ""
                )

                // If this article is not restricted.
                if (!channelItem.isMembershipRequired) {
                    return startReading(channelItem)
                }

                // If this article is restricted, check user privilege.

                // If user is not logged in, to to login.

                // If user already logged in, but not a member,
                // got to subscription.
                // Else, just startReading.

                return startReading(channelItem)
            }

            /**
             * If the path looks like `/channel/english.html`
             * On home page '每日英语' section, the title is a link
             */
            "channel" -> openChannelLink(uri)

            /**
             * If the path looks like `/tag/中美贸易战`,
             * start a new page listing articles
             */
            "tag" -> openTagLink(uri)

            /**
             * If the path looks like `/m/marketing/intelligence.html`
             */
            "m" -> openMLink(uri)

            /**
             * Loads an article that do not have JSON api and load it directly into the WebView.
             * WebContentActivity accepts a String url.
             * Example:
             * `FT研究院` is a web page loaded directly into WebView. You could only click a title's link to read this an article.
             * The link is actually pointing to somewhere like `http://www.ftchinese.com/interactive/12376`.
             * But you want it to load a stripped HTML web page on `https://api003.ftmailbox.com/interactive/12376?bodyonly=no&webview=ftcapp&i=3&0=01&exclusive`.
             * To to this, you have to transform the URL using `buildListUrl`.
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
    private fun startReading(channelItem: ChannelItem): Boolean {

        StoryActivity.start(activity, channelItem)

        return true
    }

    private fun openWebPage(uri: Uri): Boolean {
        val channelItem = ChannelItem(
                id = uri.pathSegments[1],
                type = uri.pathSegments[0],
                headline = "",
                shortlead = "")

        WebContentActivity.start(activity, channelItem)

        return true
    }

    /**
     * A list of articles
     */
    open fun openChannelLink(uri: Uri): Boolean {


        val lastPathSegment = uri.lastPathSegment

        val listPage = PagerTab(
                title = pathToTitle[lastPathSegment] ?: "",
                name = uri.pathSegments.joinToString("_").removeSuffix(".html"),
                contentUrl = buildUrlForFragment(uri),
                htmlType = PagerTab.HTML_TYPE_FRAGMENT
        )

        info("Start channel activity for link: ${listPage.contentUrl}")

        ChannelActivity.start(activity, listPage)

        return true
    }


    /**
     * Whichever pagination link user clicked, just start a ChannelActivity.
     *
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
     * For all paginiation links in a ViewPerFragment, start a ChannelActivity
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
     * This kind of page is a list of articles
     *
     *
     *
     *
     */
    open fun openMLink(uri: Uri): Boolean {
        info("Open a m link: $uri")

        return when (uri.pathSegments[1]) {
            // Links like /m/corp/preview.html?pageid=huawei2018
            "corp" -> {
                val key = uri.getQueryParameter("pageid") ?: return true

                val name = uri.pathSegments.joinToString("_").removeSuffix(".html") + "_$key"

                val listPage = PagerTab(
                        title = pathToTitle[key] ?: "",
                        name = name,
                        contentUrl = buildUrlForFullPage(uri),
                        htmlType = PagerTab.HTML_TYPE_COMPLETE
                )

                ChannelActivity.start(activity, listPage)

                true
            }
            // Links like /m/marketing/intelligence.html?webview=ftcapp
            "marketing" -> {
                val key = uri.lastPathSegment ?: ""
                val listPage = PagerTab(
                        title = pathToTitle[key] ?: "",
                        name = uri.pathSegments.joinToString("_").removeSuffix(".html"),
                        contentUrl = buildUrlForFullPage(uri),
                        htmlType = PagerTab.HTML_TYPE_COMPLETE
                )

                ChannelActivity.start(activity, listPage)

                true
            }

            else -> {
                val key = uri.lastPathSegment

                val listPage = PagerTab(
                        title = pathToTitle[key] ?: "",
                        name = "",
                        contentUrl = buildUrlForFullPage(uri),
                        htmlType = PagerTab.HTML_TYPE_COMPLETE
                )

                ChannelActivity.start(activity, listPage)

                true
            }
        }
    }

    private fun openTagLink(uri: Uri): Boolean {
        val page = PagerTab(
                title = uri.lastPathSegment,
                name = "${uri.pathSegments[0]}_${uri.pathSegments[1]}",
                contentUrl = buildUrlForFragment(uri),
                htmlType = PagerTab.HTML_TYPE_FRAGMENT
        )

        ChannelActivity.start(activity, page)

        return true
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

    private fun handleExternalLink(uri: Uri): Boolean {
        // This opens an external browser
        val customTabsInt = CustomTabsIntent.Builder().build()
        customTabsInt.launchUrl(activity, uri)

        return true
    }

    private fun buildUrlForFullPage(uri: Uri): String {
        val builder = uri.buildUpon()
        if (uri.getQueryParameter("webview") == null) {
            builder.appendQueryParameter("webview", "ftcapp")
        }

        return builder.build().toString()
    }

    private fun buildUrlForFragment(uri: Uri, path: String? = null): String {
        val builder =  uri.buildUpon()

        if (uri.getQueryParameter("bodyonly") == null) {
            builder.appendQueryParameter("bodyonly", "yes")
        }
        if (uri.getQueryParameter("webview") == null) {
            builder.appendQueryParameter("webview", "ftcapp")
        }

        if (path != null) {
            builder.path(path)
        }

        return builder.build().toString()
    }
}

