package com.ft.ftchinese

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.browser.customtabs.CustomTabsIntent
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.ft.ftchinese.models.*
import com.ft.ftchinese.ui.login.LoginActivity
import com.ft.ftchinese.ui.pay.PaywallActivity
import com.ft.ftchinese.util.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

val hostNames = arrayOf(HOST_FTC, HOST_MAILBOX)

/**
 * Those links need to start a ChannelActivity.
 * Other links include:
 * /tag/汽车未来
 */
val pathToTitle = mapOf(
        // /m/marketing/intelligence.html?webview=ftcapp
        "intelligence.html" to "FT研究院",
        // /m/marketing/businesscase.html
        "businesscase.html" to "中国商业案例精选",
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

val noAccess = mapOf(
        // /channel/english.html?webview=ftcapp
        "english.html" to "每日英语",
        // /channel/mba.html?webview=ftcapp
        "mba.html" to "FT商学院",
        // /channel/weekly.html
        "weekly.html" to "热门文章"
)

/**
 * WVClient is use mostly to handle webUrl clicks loaded into
 * ViewPagerFragment.
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
open class WVClient(
        private val activity: Activity?
) : WebViewClient(), AnkoLogger {

    // Pass click events to host.
    private var mListener: OnWebViewInteractionListener? = null

    private val sessionManager = if (activity != null) {
        SessionManager.getInstance(activity)
    } else {
        null
    }

    private fun getPrivilegeCode(): String {
        val prvl = if (activity != null) {
            val account = sessionManager?.loadAccount()

            when (account?.membership?.tier) {
                Tier.STANDARD -> """['premium']"""
                Tier.PREMIUM -> """['premium', 'EditorChoice']"""
                else -> "[]"
            }
        } else {
            "[]"
        }

        return """
        window.gPrivileges=$prvl;
        updateHeadlineLocks();
        """.trimIndent()
    }

    interface OnWebViewInteractionListener {

        // Let host to handle clicks on pagination links.
        fun onPagination(pageKey: String, pageNumber: String) {}

        fun onOpenGraphEvaluated(result: String) {}
    }

    fun setWVInteractionListener(listener: OnWebViewInteractionListener?) {
        mListener = listener
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)

        info("Start loading $url")
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)

        info("Finished loading $url")

        view?.evaluateJavascript("""
            (function() {
            ${getPrivilegeCode()}
            return window.gPrivileges;
            })()
        """.trimIndent()) {
            info("Privilege result: $it")
        }

        view?.evaluateJavascript("""
        (function getOpenGraph() {
            var metaElms = document.getElementsByTagName('meta');
            var graph = {};
            var standfirst = "";
            for (var index = 0; index < metaElms.length; index++) {
                var elm = metaElms[index];
                if (elm.hasAttribute("name")) {
                    var nameVal = elm.getAttribute("name")
                    switch (nameVal) {
                        case "keywords":
                            graph.keywords = elm.getAttribute("content");
                            break;
                        case "description":
                            standfirst = elm.getAttribute("content");
                            break;
                    }
                    continue;
                }
                if (!elm.hasAttribute('property')) {
                    continue;
                }
                var prop = elm.getAttribute('property');
                if (!prop.startsWith('og:')) {
                    continue;
                }
                var key = prop.split(":")[1];
                var value = elm.getAttribute('content');
                graph[key] = value;
            }

            if (!graph["title"]) {
                graph["title"] = document.title;
            }

            if (!graph["description"]) {
                graph["description"] = standfirst;
            }

            return graph;
        })();
        """.trimIndent()) {
            info("JS evaluation result: $it")
            mListener?.onOpenGraphEvaluated(it)
        }
    }

    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        info("Error when requesting ${request?.url}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            info("Error code: ${error?.errorCode}, description: ${error?.description}")
        }
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {

        val uri = request?.url ?: return true

        info("shouldOverrideUrlLoading: $uri")

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

            // The `免费注册` button is wrapped in a link with webUrl set to `ftcregister://www.ftchinese.com/`
            // The `微信登录` button is wrapped in a link with webUrl set to `weixinlogin://www.ftchinese.com/`
            "ftcregister",
            "weixinlogin" -> {
                LoginActivity.startForResult(activity)
                return true
            }

            /**
             * If the clicked webUrl is of the pattern `.../story/xxxxxx`, you should use `StoryActivity`
             * and fetch JSON from server and concatenate it with a html bundle into the package `raw/story.html`,
             * then call `WebView.loadDataWithBaseUrl()` to load the string into WebView.
             * In such a case, you need to provide a base webUrl so that contents in the WebView know where to fetch resources (like advertisement).
             * The base webUrl for such contents should be `www.ftchinese.com`.
             * If you load a webUrl directly into, the host might be something else, like `api003.ftmailbox.com`, depending your webUrl you use.
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
            val ccode = uri.getQueryParameter("ccode")
            if (ccode == null) {
                PaywallTracker.from = null
            } else {
                PaywallTracker.from = PaywallSource(
                        id = ccode,
                        type = "promotion",
                        title = "subscription.html",
                        category = GACategory.SUBSCRIPTION,
                        action = GAAction.DISPLAY,
                        label = "fta/subscription.html"
                )
            }

            PaywallActivity.start(context = activity)
        }

        return true
    }

    private fun handleInSiteLink(uri: Uri): Boolean {

        info("Handle in-site link")

        val pathSegments = uri.pathSegments

        info("Path segments: $pathSegments")

        /**
         * Handle pagination links.
         * What action to preform depends on whether you
         *
         * Whichever pagination link user clicked, just start a ChannelActivity.
         *
         * Handle the pagination link of each channel
         * There's a problem with each channel's pagination: they used relative urls.
         * When loaded in WebView with base webUrl `http://www.ftchinese.com`,
         * the webUrl will become something `http://www.ftchinese.com/china.html?page=2`,
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
             * webUrl. No more information could be acquired.
             */
            ChannelItem.TYPE_STORY,
            ChannelItem.TYPE_PREMIUM -> {
                val lastPathSegment = uri.lastPathSegment ?: return true
                val channelItem = ChannelItem(
                        id = lastPathSegment,
                        type = pathSegments[0],
                        title = "",
                        webUrl = uri.toString()
                )

                ArticleActivity.start(activity, channelItem)

                true
            }
            ChannelItem.TYPE_VIDEO,
            // Links on home page under FT商学院
            ChannelItem.TYPE_PHOTO_NEWS,
            // Links on home page under FT研究院
            ChannelItem.TYPE_INTERACTIVE -> {

                val lastPathSegment = uri.lastPathSegment ?: return true
                val channelItem = ChannelItem(
                        id = lastPathSegment,
                        type = pathSegments[0],
                        title = "",
                        webUrl = uri.toString()
                )

                ArticleActivity.startWeb(activity, channelItem)
                true
            }

            /**
             * Load content in into [ChannelActivity].
             * If the path looks like `/channel/english.html`
             * On home page '每日英语' section, the title is a link
             * Similar to TYPE_COLUMN
             * Should load a full webpage without header under such cases.
             * Editor choice also use /channel path. You should handle it separately.
             * When a links is clicked on Editor choice, retrieve a HTML fragment.
             */
            ChannelItem.TYPE_CHANNEL -> openChannelLink(uri)

            /**
             * If the path looks like `/m/marketing/intelligence.html`
             * or /m/corp/preview.html?pageid=huawei2018
             */
            ChannelItem.TYPE_M -> openMLink(uri)

            /**
             * If the path looks like `/tag/中美贸易战`, `/archiver/2019-03-05`
             * start a new page listing articles
             */
            ChannelItem.TYPE_TAG,
            ChannelItem.TYPE_ARCHIVE -> {
                val page = ChannelSource(
                        title = uri.lastPathSegment ?: "",
                        name = uri.pathSegments.joinToString("_"),
                        contentUrl = buildUrlForFragment(uri),
                        htmlType = HTML_TYPE_FRAGMENT
                )

                ChannelActivity.start(activity, page)

                true
            }

            else -> {
                info("Open a web page directly. Original webUrl is: $uri")
                val chSrc = ChannelSource(
                        title = uri.lastPathSegment ?: "",
                        name = "",
                        contentUrl = uri.toString(),
                        htmlType = HTML_TYPE_COMPLETE
                )

                ChannelActivity.start(activity, chSrc)

                true
            }
        }
    }

    /**
     * Handle paths like:
     * `/channel/editorchoice-issue.html?issue=EditorChoice-xxx`,
     * `/channel/chinabusinesswatch.html`
     * `/channel/viewtop.html`
     * `/channel/teawithft.html`
     * `/channel/markets.html`
     * `/channel/money.html`
    */
    private fun openChannelLink(uri: Uri): Boolean {
        info("Open a channel link: $uri")

        val lastPathSegment = uri.lastPathSegment ?: return true

        // Prevent multiple entry point for a single item.
        if (noAccess.containsKey(lastPathSegment)) {
            return true
        }

        when (lastPathSegment) {
            // Handle links on this page: https://api003.ftmailbox.com/channel/editorchoice.html?webview=ftcapp&bodyonly=yes&ad=no&showEnglishAudio=yes&018
            // The link itself looks like:
            // http://www.ftchinese.com/channel/editorchoice-issue.html?issue=EditorChoice-20181105
            "editorchoice-issue.html" -> {
                info("Clicked an editor choice link: $uri")
                val issue = uri.getQueryParameter("issue")
                        ?: uri.pathSegments.joinToString("_").removeSuffix(".html")

                val channelSource = ChannelSource(
                        title = pathToTitle[lastPathSegment] ?: "",
                        name = issue,
                        contentUrl = buildUrlForFragment(uri),
                        htmlType = HTML_TYPE_FRAGMENT,
                        permission = Permission.PREMIUM
                )

                info("Channel source for editor choice: $channelSource")
                ChannelActivity.start(activity, channelSource)
            }
            else -> {

                val listPage = ChannelSource(
                        title = pathToTitle[lastPathSegment] ?: "",
                        name = uri.pathSegments.joinToString("_").removeSuffix(".html"),
                        contentUrl = buildUrlForFragment(uri),
                        htmlType = HTML_TYPE_FRAGMENT
                )

                info("Start channel activity for $listPage")

                ChannelActivity.start(activity, listPage)
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

                val listPage = ChannelSource(
                        title = if (pageName != null) pathToTitle[pageName] ?: "" else "",
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
                val listPage = ChannelSource(
                        title = pathToTitle[key] ?: "",
                        name = uri.pathSegments.joinToString("_").removeSuffix(".html"),
                        contentUrl = buildUrlForFullPage(uri),
                        htmlType = HTML_TYPE_COMPLETE
                )

                ChannelActivity.start(activity, listPage)

                true
            }

            else -> {
                val key = uri.lastPathSegment ?: ""

                val listPage = ChannelSource(
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
        val pm = activity?.packageManager ?: return true

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, "ftchinese.feedback@gmail.com")
            putExtra(Intent.EXTRA_SUBJECT, "Feedback on FTC Android App")
        }

        return if (intent.resolveActivity(pm) != null) {
            activity.startActivity(intent)
            true
        } else {
            activity.toast(R.string.prompt_no_email_app)
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
                .scheme("https")
                .authority(HOST_MAILBOX)

        if (uri.getQueryParameter("webview") == null) {
            builder.appendQueryParameter("webview", "ftcapp")
        }

        return builder.build().toString()
    }

    private fun buildUrlForFragment(uri: Uri, path: String? = null): String {
        val builder =  uri.buildUpon()
                .scheme("https")
                .authority(HOST_MAILBOX)

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

