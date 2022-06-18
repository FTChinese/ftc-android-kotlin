package com.ft.ftchinese.ui.web

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.WebView
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.model.content.ChannelSource
import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.model.legal.WebpageMeta
import com.ft.ftchinese.model.request.WxMiniParams
import com.ft.ftchinese.repository.Config
import com.ft.ftchinese.tracking.GAAction
import com.ft.ftchinese.tracking.GACategory
import com.ft.ftchinese.tracking.PaywallSource
import com.ft.ftchinese.tracking.PaywallTracker
import com.ft.ftchinese.ui.article.ArticleActivity
import com.ft.ftchinese.ui.article.ChannelActivity
import com.ft.ftchinese.ui.auth.AuthActivity
import com.ft.ftchinese.ui.util.toast
import com.ft.ftchinese.ui.subs.SubsActivity
import com.ft.ftchinese.ui.util.IntentsUtil
import com.ft.ftchinese.ui.webpage.WebpageActivity
import com.tencent.mm.opensdk.modelbiz.WXLaunchMiniProgram
import com.tencent.mm.opensdk.openapi.WXAPIFactory

private const val TAG = "WebClient"
private const val keyWxMiniId = "wxminiprogramid"
private const val keyWxMiniPath = "wxminiprogrampath"

fun containWxMiniProgram(url: Uri): Boolean {
    return !url.getQueryParameter(keyWxMiniId).isNullOrBlank()
}

sealed class WvUrlEvent {
    // Open email program
    data class MailTo(val uri: Uri) : WvUrlEvent()
    // Go to AuthActivity
    object Login : WvUrlEvent()
    // Open wechat mini program
    data class WxMiniProgram(val params: WxMiniParams) : WvUrlEvent()

    data class Article(val teaser: Teaser) : WvUrlEvent()
    data class Channel(val source: ChannelSource) : WvUrlEvent()
    data class Pagination(val paging: Paging) : WvUrlEvent()
    // Open in WebpageActivity
    data class UnknownInSite(val uri: Uri): WvUrlEvent()

    // Go to SubsActivity
    object FtaSubs : WvUrlEvent()

    // Open in custom tabs
    data class External(val uri: Uri) : WvUrlEvent()

    companion object {
        @JvmStatic
        fun fromUri(uri: Uri): WvUrlEvent {
            return when (uri.scheme) {
                // 通过邮件反馈 link: mailto:ftchinese.feedback@gmail.com?subject=Feedback
                "mailto" -> MailTo(uri)

                // The `免费注册` button is wrapped in a link with webUrl set to `ftcregister://www.ftchinese.com/`
                // The `微信登录` button is wrapped in a link with webUrl set to `weixinlogin://www.ftchinese.com/`
                "ftcregister",
                "weixinlogin" -> Login

                "ftchinese" -> {
                    if (uri.pathSegments.size > 0 && uri.pathSegments[0] != ArticleKind.story) {
                        Article(teaserFromFtcSchema(uri))
                    } else {
                        UnknownInSite(uri.buildUpon().scheme("https").build())
                    }
                }
                /**
                 * If the clicked webUrl is of the pattern `.../story/xxxxxx`, you should use `StoryActivity`
                 * and fetch JSON from server and concatenate it with a html bundle into the package `raw/story.html`,
                 * then call `WebView.loadDataWithBaseUrl()` to load the string into WebView.
                 * In such a case, you need to provide a base webUrl so that contents in the WebView know where to fetch resources (like advertisement).
                 */
                "http", "https" -> when {
                    containWxMiniProgram(uri) -> ofWxMiniProgram(uri)
                    Config.isInternalLink(uri.host ?: "") -> ofInSiteLink(uri)
                    Config.isFtaLink(uri.host ?: "") -> ofFtaSubs(uri)
                    else -> External(uri)
                }
                // For unknown schemes, simply returns true to prevent
                // crash caused by loading unknown content.
                else -> External(uri)
            }
        }

        @JvmStatic
        private fun ofWxMiniProgram(uri: Uri): WvUrlEvent {
            val wxMiniProgramId = uri.getQueryParameter(keyWxMiniId) ?: return External(uri)


            val params = WxMiniParams(
                id = wxMiniProgramId,
                path = uri.getQueryParameter(keyWxMiniPath) ?: ""
            )

            return WxMiniProgram(params)
        }

        @JvmStatic
        /**
         * Handle urls like:
         * http://www.ftacademy.cn/subscription.html?ccode=ftchomepromobox
         */
        private fun ofFtaSubs(uri: Uri): WvUrlEvent {
            if (uri.lastPathSegment != "subscription.html") {
                return UnknownInSite(uri)
            }

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

            return FtaSubs
        }

        @JvmStatic
        private fun ofInSiteLink(uri: Uri): WvUrlEvent {

            val pathSegments = uri.pathSegments

            Log.i(TAG, "Handle in-site link. Path segments: $pathSegments")

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
                Log.i(TAG, "Open channel pagination for uri: $uri")

                val paging = Paging(
                    key = if (uri.getQueryParameter("page") != null)
                        "page"
                    else
                        "p",
                    page = pageNumber
                )

                // Since the pagination query parameter's key is not uniform across whole site, we have to explicitly tells host.
                // Let host activity/fragment to handle pagination link
                Pagination(paging)
            }

            // In case no path segments
            if (pathSegments.size == 0) {
                return UnknownInSite(uri)
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
                ArticleKind.story,
                ArticleKind.premium,
                ArticleKind.video,
                // Links on home page under FT商学院
                ArticleKind.photoNews,
                // Links on home page under FT研究院
                ArticleKind.interactive -> Article(teaserFromUri(uri))

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
                ArticleKind.channel -> Channel(channelFromUri(uri))

                /**
                 * This kind of page is a list of articles
                 * If the path looks like `/m/marketing/intelligence.html`
                 * or /m/corp/preview.html?pageid=huawei2018
                 */
                ArticleKind.m -> Channel(marketingChannelFromUri(uri))

                /**
                 * If the path looks like `/tag/中美贸易战`, `/archiver/2019-03-05`
                 * start a new page listing articles
                 */
                ArticleKind.tag,
                ArticleKind.archive -> Channel(tagOrArchiveChannel(uri))
                else -> UnknownInSite(uri)
            }
        }
    }
}

open class WebViewCallback(
    private val context: Context,
    private val channelSource: ChannelSource? = null
) {

    open fun onPageStarted(view: WebView?, url: String?) {
        Log.i(TAG, "Start loading $url")
    }

    open fun onPageFinished(view: WebView?, url: String?) {
        Log.i(TAG, "Finished loading $url")
    }

    /**
     * Used when we are loading an html without knowing enough
     * metadata to determine the article's permission.
     */
//    open fun openGraphEvaluated(openGraph: OpenGraphMeta) {}

    fun onOverrideUrlLoading(event: WvUrlEvent) {
        when (event) {
            is WvUrlEvent.MailTo -> {
                val ok = IntentsUtil.sendEmail(
                    context = context,
                    uri = event.uri
                )
                if (!ok) {
                    context.toast(R.string.prompt_no_email_app)
                }
            }
            is WvUrlEvent.Login -> {
                onLogin()
            }
            is WvUrlEvent.WxMiniProgram -> {
                launchWxMiniProgram(
                    context,
                    event.params
                )
            }
            is WvUrlEvent.Article -> {
                onClickStory(event.teaser)
            }
            is WvUrlEvent.Channel -> {
                onClickChannel(event.source)
            }
            is WvUrlEvent.Pagination -> {
                onPagination(event.paging)
            }
            is WvUrlEvent.UnknownInSite -> {
                WebpageActivity.start(
                    context,
                    WebpageMeta(
                        title = "",
                        url = event.uri.toString()
                    )
                )
            }
            is WvUrlEvent.FtaSubs -> {
                SubsActivity.start(context)
            }
            is WvUrlEvent.External -> {
                launchCustomTabs(
                    context,
                    event.uri
                )
            }
        }
    }

    // When pagination link in a channel page is clicked
    open fun onPagination(paging: Paging) {
        Log.i(TAG, "Pagination: $paging")
        val pagedSource = channelSource?.withPagination(
            pageKey = paging.key,
            pageNumber = paging.page
        ) ?: return

        if (channelSource.isSamePage(pagedSource)) {
            return
        }

        onClickChannel(pagedSource)
    }

    // When a link in web view point to a channel
    // A channel link is handled differently based on where it is hosted.
    // When inside a ChannelActivity, add permissions from
    // hosting activity.
    // When inside an ArticleActivity, open it directly since you have no way to know its parent permission.
    // This is not a correct approach since server should provide
    // enough information about permissions.
    // Alas they didn't.
    open fun onClickChannel(source: ChannelSource) {
        ChannelActivity.start(
            context,
            source.withParentPerm(channelSource?.permission)
        )
    }

    open fun onClickStory(teaser: Teaser) {
        ArticleActivity.start(
            context,
            teaser.withParentPerm(channelSource?.permission)
        )
    }

    open fun onLogin() {
        context.startActivity(AuthActivity.newIntent(context))
    }
}

@Composable
fun rememberWebViewCallback(
    context: Context = LocalContext.current,
    channelSource: ChannelSource? = null
) = remember(channelSource) {
    WebViewCallback(
        context = context,
        channelSource = channelSource,
    )
}

private fun launchWxMiniProgram(
    context: Context,
    params: WxMiniParams
) {
    WXAPIFactory
        .createWXAPI(context, BuildConfig.WX_SUBS_APPID, false)
        .sendReq(
            WXLaunchMiniProgram.Req().apply {
                userName = params.id
                path = params.path
                miniprogramType = if (BuildConfig.DEBUG) {
                    WXLaunchMiniProgram.Req.MINIPROGRAM_TYPE_TEST
                } else {
                    WXLaunchMiniProgram.Req.MINIPTOGRAM_TYPE_RELEASE
                }
            }
        )
}

fun launchCustomTabs(ctx: Context, url: Uri) {
    CustomTabsIntent
        .Builder()
        .build()
        .launchUrl(
            ctx,
            url
        )
}
