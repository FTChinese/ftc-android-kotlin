package com.ft.ftchinese.ui.base

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.browser.customtabs.CustomTabsIntent
import com.ft.ftchinese.R
import com.ft.ftchinese.model.content.*
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.repository.Config
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.tracking.GAAction
import com.ft.ftchinese.tracking.GACategory
import com.ft.ftchinese.tracking.PaywallSource
import com.ft.ftchinese.tracking.PaywallTracker
import com.ft.ftchinese.ui.article.ArticleActivity
import com.ft.ftchinese.ui.article.WebViewActivity
import com.ft.ftchinese.ui.login.LoginActivity
import com.ft.ftchinese.ui.paywall.PaywallActivity
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

private const val TYPE_STORY = "story"
private const val TYPE_PREMIUM = "premium"
private const val TYPE_VIDEO = "video"
private const val TYPE_INTERACTIVE = "interactive"
private const val TYPE_PHOTO_NEWS = "photonews"
private const val TYPE_CHANNEL = "channel"
private const val TYPE_TAG = "tag"
private const val TYPE_M = "m"
private const val TYPE_ARCHIVE = "archiver"

/**
 * WVClient is use mostly to handle webUrl clicks loaded into
 * ViewPagerFragment.
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
open class WVClient(
        private val context: Context,
        private val viewModel: WVViewModel? = null
) : WebViewClient(), AnkoLogger {

    private val sessionManager = SessionManager.getInstance(context)

    private fun getPrivilegeCode(): String {
        val account = sessionManager.loadAccount()

        val prvl = when (account?.membership?.tier) {
            Tier.STANDARD -> """['premium']"""
            Tier.PREMIUM -> """['premium', 'EditorChoice']"""
            else -> "[]"
        }

        return """
        window.gPrivileges=$prvl;
        updateHeadlineLocks();
        """.trimIndent()
    }


    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)

        info("Start loading $url")
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        info("Finished loading $url")

        info("WVViewModel $viewModel")
        viewModel?.pageFinished?.value = true


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
            viewModel?.openGraphEvaluated?.value = it
//            mListener?.onOpenGraphEvaluated(it)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        info("Error when requesting ${request?.url}")

        info("Error code: ${error?.errorCode}, description: ${error?.description}")
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
                LoginActivity.start(context)
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
                return when {
                    Config.isInternalLink(uri.host ?: "") -> handleInSiteLink(uri)
                    Config.isFtaLink(uri.host ?: "") -> handleFtaLink(uri)
                    else -> handleExternalLink(uri)
                }
            }
            // For unknown schemes, simply returns true to prevent
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

            PaywallActivity.start(context = context)
        }

        return true
    }

    private fun handleInSiteLink(uri: Uri): Boolean {

        val pathSegments = uri.pathSegments

        info("Handle in-site link. Path segments: $pathSegments")

        /**
         * Handle pagination links.
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
         * For all pagination links in a ViewPerFragment, start a ChannelActivity
        */
        val pageNumber = uri.getQueryParameter("page")
            ?: uri.getQueryParameter("p")

        if (pageNumber != null) {
            info("Open channel pagination for uri: $uri")

            val paging = Paging(
                key = if (uri.getQueryParameter("page") != null)
                        "page"
                    else
                        "p",
                page = pageNumber
            )

            // Since the pagination query parameter's key is not uniform across whole site, we have to explicitly tells host.
            // Let host activity/fragment to handle pagination link
//            mListener?.onPagination(paging)

            viewModel?.pagingBtnClicked?.value = paging

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
            TYPE_STORY,
            TYPE_PREMIUM,
            TYPE_VIDEO,
                // Links on home page under FT商学院
            TYPE_PHOTO_NEWS,
                // Links on home page under FT研究院
            TYPE_INTERACTIVE -> {

                ArticleActivity.start(context, buildTeaserFromUri(uri))
                true
            }

            /**
             * Load content in into ChannelActivity.
             * If the path looks like `/channel/english.html`
             * On home page '每日英语' section, the title is a link
             * Similar to TYPE_COLUMN
             * Should load a full webpage without header under such cases.
             * Editor choice also use /channel path. You should handle it separately.
             * When a links is clicked on Editor choice, retrieve a HTML fragment.
             * Handle paths like:
             * `/channel/editorchoice-issue.html?issue=EditorChoice-xxx`,
             * `/channel/chinabusinesswatch.html`
             * `/channel/viewtop.html`
             * `/channel/teawithft.html`
             * `/channel/markets.html`
             * `/channel/money.html`
             */
            TYPE_CHANNEL -> {
                info("Open a channel link: $uri")

                val lastPathSegment = uri.lastPathSegment ?: return true

                // Prevent multiple entry point for a single item.
//                if (noAccess.containsKey(lastPathSegment)) {
//                    true
//                } else {
//                    info("Open a new channel. Path: ${uri.path}")
//                    viewModel?.urlChannelSelected?.value = buildChannelFromUri(uri)
//                    true
//                }

                info("Open a new channel. Path: ${uri.path}")
                viewModel?.urlChannelSelected?.value = buildChannelFromUri(uri)
                true
            }

            /**
             * This kind of page is a list of articles
             * If the path looks like `/m/marketing/intelligence.html`
             * or /m/corp/preview.html?pageid=huawei2018
             */
            TYPE_M -> {
                info("Loading marketing page")

                viewModel?.urlChannelSelected?.value = buildMarketingChannel(uri)

                true
            }

            /**
             * If the path looks like `/tag/中美贸易战`, `/archiver/2019-03-05`
             * start a new page listing articles
             */
            TYPE_TAG,
            TYPE_ARCHIVE -> {
                info("Loading tag or archive")
                viewModel?.urlChannelSelected?.value = buildTagOrArchiveChannel(uri)
                true
            }

            else -> {
                info("Loading a plain web page")
                WebViewActivity.start(context, uri.toString())
                true
            }
        }
    }

    private fun feedbackEmail(): Boolean {
        val pm = context.packageManager ?: return true

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, "ftchinese.feedback@gmail.com")
            putExtra(Intent.EXTRA_SUBJECT, "Feedback on FTC Android App")
        }

        return if (intent.resolveActivity(pm) != null) {
            context.startActivity(intent)
            true
        } else {
            context.toast(R.string.prompt_no_email_app)
            true
        }
    }

    private fun handleExternalLink(uri: Uri): Boolean {
        // This opens an external browser
        val customTabsInt = CustomTabsIntent.Builder().build()
        customTabsInt.launchUrl(context, uri)


        return true
    }
}

