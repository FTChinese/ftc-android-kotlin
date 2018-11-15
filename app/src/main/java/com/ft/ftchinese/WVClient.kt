package com.ft.ftchinese

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.support.customtabs.CustomTabsIntent
import android.webkit.WebView
import android.webkit.WebViewClient
import com.ft.ftchinese.models.*
import com.ft.ftchinese.user.SignInActivity
import com.ft.ftchinese.user.SignUpActivity
import com.ft.ftchinese.user.SubscriptionActivity
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

/**
 * WVClient is use mostly to handle url clicks loaded into
 * ViewPagerFragment.
 */
class WVClient(
        private val activity: Activity?
) : WebViewClient(), AnkoLogger {

    var mSession: SessionManager? = null

    // Pass click events to host.
    private var mListener: OnClickListener? = null

    interface OnClickListener {

        // Let host to handle clicks on pagination links.
        fun onPagination(pageKey: String, pageNumber: String)
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
            "mailto" -> feedbackEmail()

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
                if (hostNames.contains(uri.host)) {
                    return handleInSiteLink(uri)
                }
                return when (uri.host) {
                    HOST_FTC, HOST_MAILBOX -> handleInSiteLink(uri)
                    HOST_FTA -> handleFtaLink(uri)
                    else -> handleExternalLink(uri)
                }
            }
            // For unknown links, simply returns true to prevent
            // crash caused by loading unknown content.
            else -> true
        }
    }


    /**
     * Handle urls like:
     * http://www.ftacademy.cn/subscription.html?ccode=ftchomepromobox
     */
    private fun handleFtaLink(uri: Uri): Boolean {
        if (uri.lastPathSegment == "subscription.html") {
            SubscriptionActivity.start(activity)
        }

        return true
    }

    private fun handleInSiteLink(uri: Uri): Boolean {
        val pathSegments = uri.pathSegments

        info("Handle in-site link $uri")
        /**
         * Handle pagination links.
         * What action to preform depends on whether you
         *
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
        if (uri.getQueryParameter("page") != null || uri.getQueryParameter("p") != null) {
            info("Open channel pagination for uri: $uri")

            val pageValue = uri.getQueryParameter("page")
                ?: uri.getQueryParameter("p")
                ?: return true

            // Since the pagination query parameter's key is not uniform across whole site, we have to explicitly tells host.
            val pageKey = if (uri.getQueryParameter("page") != null) "page"
            else "p"

            // Let host activity/fragment to handle pagination link
            mListener?.onPagination(pageKey, pageValue)

            return true
        }

        /**
         * URL needs to be handled on home page
         * 每日英语 /channel/english.html?webview=ftcapp
         * FT商学院 /channel/mba.html?webview=ftcapp
         * FT商学院 /photonews/1082 articles under it
         * FT研究院 /m/marketing/intelligence.html?webview=ftcapp
         * FT研究院 /interactive/12781 article under it.
         * 热门文章 /channel/weekly.html
         * 热门文章 /story/xxxx articles under it.
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
             * We could only get an article's type and id from
             * url. No more information could be acquired.
             */
            ChannelItem.TYPE_STORY,
            ChannelItem.TYPE_PREMIUM,
            ChannelItem.TYPE_VIDEO,
            ChannelItem.TYPE_INTERACTIVE,
            ChannelItem.TYPE_PHOTO_NEWS -> {
                val channelItem = ChannelItem(
                        id = pathSegments[1],
                        type = pathSegments[0],
                        headline = ""
                )

                // If this article is not restricted.
                // The logic here is similar to JSInterface#selectitem.
                // It seems there's no way to merge them together
                // since js event to activities, and url links to activities are all many-to-many relationship.
                if (!channelItem.isMembershipRequired) {
                    return startReading(channelItem)
                }

                val account = mSession?.loadUser()

                if (account == null) {
                    activity?.toast(R.string.prompt_restricted_paid_user)
                    SignInActivity.startForResult(activity)

                    return true
                }

                if (!account.canAccessPaidContent) {
                    activity?.toast(R.string.prompt_restricted_paid_user)
                    SubscriptionActivity.start(activity)

                    return true
                }

                return startReading(channelItem)
            }

            /**
             * If the path looks like `/channel/english.html`
             * On home page '每日英语' section, the title is a link
             * Similar to TYPE_COLUMN
             */
            ChannelItem.TYPE_CHANNEL -> {
                val lastPathSegment = uri.lastPathSegment

                val listPage = PagerTab(
                        title = pathToTitle[lastPathSegment] ?: "",
                        name = uri.pathSegments.joinToString("_").removeSuffix(".html"),
                        contentUrl = buildUrlForFragment(uri),
                        htmlType = HTML_TYPE_FRAGMENT
                )

                info("Start channel activity for link: ${listPage.contentUrl}")

                ChannelActivity.start(activity, listPage)

                return true
            }

            /**
             * If the path looks like `/tag/中美贸易战`,
             * start a new page listing articles
             */
            ChannelItem.TYPE_TAG -> {
                val page = PagerTab(
                        title = uri.lastPathSegment,
                        name = uri.pathSegments.joinToString("_"),
                        contentUrl = buildUrlForFragment(uri),
                        htmlType = HTML_TYPE_FRAGMENT
                )

                ChannelActivity.start(activity, page)

                return true
            }

            /**
             * If the path looks like `/m/marketing/intelligence.html`
             * or /m/corp/preview.html?pageid=huawei2018
             */
            ChannelItem.TYPE_M -> openMLink(uri)

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

        when (channelItem.type) {
            ChannelItem.TYPE_STORY, ChannelItem.TYPE_PREMIUM -> {
                StoryActivity.start(activity, channelItem)
            }
            else -> {
                WebContentActivity.start(activity, channelItem)
            }
        }
        return true
    }

    /**
     * This kind of page is a list of articles
     */
    private fun openMLink(uri: Uri): Boolean {
        info("Open a m link: $uri")

        return when (uri.pathSegments[1]) {
            // Links like /m/corp/preview.html?pageid=huawei2018
            ChannelItem.SUB_TYPE_CORP -> {
                val pageName = uri.getQueryParameter("pageid")

                val name = if (pageName != null) {
                    uri.pathSegments.joinToString("_").removeSuffix(".html") + "_$pageName"
                } else {
                    uri.pathSegments.joinToString("_").removeSuffix(".html")
                }

                val listPage = PagerTab(
                        title = pathToTitle[pageName] ?: "",
                        name = name,
                        contentUrl = buildUrlForFullPage(uri),
                        htmlType = HTML_TYPE_COMPLETE
                )

                ChannelActivity.start(activity, listPage)

                true
            }
            // Links like /m/marketing/intelligence.html?webview=ftcapp
            ChannelItem.SUB_TYPE_MARKETING -> {
                val key = uri.lastPathSegment ?: ""
                val listPage = PagerTab(
                        title = pathToTitle[key] ?: "",
                        name = uri.pathSegments.joinToString("_").removeSuffix(".html"),
                        contentUrl = buildUrlForFullPage(uri),
                        htmlType = HTML_TYPE_COMPLETE
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
                        htmlType = HTML_TYPE_COMPLETE
                )

                ChannelActivity.start(activity, listPage)

                true
            }
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

