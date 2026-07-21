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
import com.ft.ftchinese.model.content.WebpageMeta
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.request.WxMiniParams
import com.ft.ftchinese.repository.HostConfig
import com.ft.ftchinese.tracking.GAAction
import com.ft.ftchinese.tracking.GACategory
import com.ft.ftchinese.tracking.PaywallSource
import com.ft.ftchinese.tracking.PaywallTracker
import com.ft.ftchinese.ui.article.ArticleActivity
import com.ft.ftchinese.ui.article.ChannelActivity
import com.ft.ftchinese.ui.auth.AuthActivity
import com.ft.ftchinese.ui.util.toast
import com.ft.ftchinese.ui.subs.SubsActivity
import com.ft.ftchinese.ui.subs.SubscriptionEntryIntent
import com.ft.ftchinese.ui.util.IntentsUtil
import com.ft.ftchinese.ui.webpage.WebpageActivity
import com.tencent.mm.opensdk.modelbiz.WXLaunchMiniProgram
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import java.util.Locale

private const val TAG = "WebClient"
const val WEB_PURCHASE_FLOW_TAG = "FTCPurchaseFlow"
private const val keyWxMiniId = "wxminiprogramid"
private const val keyWxMiniPath = "wxminiprogrampath"
private val safeCampaignValue = Regex("^[A-Za-z0-9._:-]{1,128}$")
private val safeOfferValue = Regex("^[A-Za-z0-9._:-]{1,128}$")
private val safePriceHintValue = Regex("^[0-9]+(?:\\.[0-9]{1,2})?$")
private val sensitiveQueryKeys = setOf(
    "access_token",
    "token",
    "password",
    "code",
    "openid",
    "unionid",
    // GAM click identifiers can be very long and should not be copied from logs.
    "xai",
    "sai",
    "sig",
    "fbs_aeid",
)
private val gamLandingParameterNames = setOf("adurl", "url")

fun debugWebUrl(uri: Uri?): String {
    if (uri == null) {
        return ""
    }

    return runCatching {
        val queryNames = uri.queryParameterNames
        if (queryNames.isEmpty()) {
            return@runCatching uri.toString()
        }

        val builder = uri.buildUpon().clearQuery()
        queryNames.sorted().forEach { key ->
            val values = uri.getQueryParameters(key)
            if (values.isEmpty()) {
                builder.appendQueryParameter(key, "")
            } else {
                values.forEach { value ->
                    builder.appendQueryParameter(
                        key,
                        if (sensitiveQueryKeys.contains(key.lowercase(Locale.US))) {
                            "<redacted>"
                        } else {
                            value.orEmpty().take(256)
                        }
                    )
                }
            }
        }

        builder.build().toString()
    }.getOrElse {
        uri.toString()
    }
}

fun debugWebUrl(url: String?): String {
    if (url.isNullOrBlank()) {
        return ""
    }

    return runCatching {
        debugWebUrl(Uri.parse(url))
    }.getOrElse {
        url.take(2048)
    }
}

fun containWxMiniProgram(url: Uri): Boolean {
    return !url.getQueryParameter(keyWxMiniId).isNullOrBlank()
}

fun sanitizedCampaignValue(value: String?): String? {
    val trimmed = value?.trim().orEmpty()
    if (trimmed.isBlank() || trimmed.length > 128) {
        return null
    }

    return trimmed.takeIf { safeCampaignValue.matches(it) }
}

private fun sanitizedOfferValue(value: String?): String? {
    val trimmed = value?.trim().orEmpty()
    if (trimmed.isBlank() || trimmed.length > 128) {
        return null
    }

    return trimmed.takeIf { safeOfferValue.matches(it) }
}

private fun isNativeLoginEntry(uri: Uri): Boolean {
    if (!HostConfig.isTrustedAuthHost(uri.host)) {
        return false
    }

    val normalizedPath = uri.path?.trimEnd('/')?.lowercase(Locale.US) ?: return false
    return normalizedPath == "/login" || normalizedPath == "/login/safe_mode"
}

private fun sanitizedPriceHintValue(value: String?): String? {
    val trimmed = value?.trim().orEmpty()
    if (trimmed.isBlank() || trimmed.length > 32) {
        return null
    }

    return trimmed.takeIf { safePriceHintValue.matches(it) }
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
    data class CorpPage(val uri: Uri) : WvUrlEvent()
    // Open in WebpageActivity
    data class UnknownInSite(val uri: Uri): WvUrlEvent()

    // Go to SubsActivity while preserving campaign attribution and discount source.
    data class FtaSubs(
        val uri: Uri,
        val ccode: String?,
        val from: String?,
    ) : WvUrlEvent()
    data class Subscribe(
        val uri: Uri,
        val tier: Tier?,
        val ccode: String?,
        val from: String?,
        val offerHint: String?,
        val priceHint: String?,
        val sourceScheme: String,
    ) : WvUrlEvent()

    // Open in custom tabs
    data class External(val uri: Uri) : WvUrlEvent()

    /** A GAM click wrapper with the market team's explicit in-app marker. */
    data class CampaignAd(
        val uri: Uri,
        val landingUri: Uri?,
        val ccode: String,
    ) : WvUrlEvent()

    companion object {
        @JvmStatic
        fun fromUri(uri: Uri): WvUrlEvent {
            val event = when (uri.scheme?.lowercase(Locale.US)) {
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
                "subscribe" -> ofSubscribe(uri)
                /**
                 * If the clicked webUrl is of the pattern `.../story/xxxxxx`, you should use `StoryActivity`
                 * and fetch JSON from server and concatenate it with a html bundle into the package `raw/story.html`,
                 * then call `WebView.loadDataWithBaseUrl()` to load the string into WebView.
                 * In such a case, you need to provide a base webUrl so that contents in the WebView know where to fetch resources (like advertisement).
                 */
                "http", "https" -> when {
                    containWxMiniProgram(uri) -> ofWxMiniProgram(uri)
                    isGamCampaignLink(uri) -> ofGamCampaignLink(uri)
                    isFtContentLink(uri) -> Article(teaserFromUri(uri))
                    isWebSubscriptionEntry(uri) -> ofWebSubscription(uri)
                    HostConfig.isFtaLink(uri.host ?: "") -> ofFtaSubs(uri)
                    isCorpPreview(uri) -> {
                        Log.i(
                            WEB_PURCHASE_FLOW_TAG,
                            "match_campaign_corp_preview host=${uri.host.orEmpty()} " +
                                "url=${debugWebUrl(uri)}"
                        )
                        ofInSiteLink(uri)
                    }
                    HostConfig.isInternalLink(uri.host ?: "") || HostConfig.isTrustedAuthHost(uri.host) -> ofInSiteLink(uri)
                    else -> External(uri)
                }
                // For unknown schemes, simply returns true to prevent
                // crash caused by loading unknown content.
                null, "" -> when {
                    isWebSubscriptionEntry(uri) -> ofWebSubscription(uri)
                    else -> External(uri)
                }
                else -> External(uri)
            }

            Log.i(
                WEB_PURCHASE_FLOW_TAG,
                "classify route=${event.debugRouteName()} " +
                    "scheme=${uri.scheme.orEmpty()} host=${uri.host.orEmpty()} " +
                    "path=${uri.path.orEmpty()} url=${debugWebUrl(uri)}"
            )
            return event
        }

        private fun isFtContentLink(uri: Uri): Boolean {
            val host = uri.host ?: return false
            val isFtHost = host.equals("www.ft.com", ignoreCase = true) ||
                host.equals("ft.com", ignoreCase = true)
            return isFtHost && uri.pathSegments.firstOrNull() == ArticleKind.content
        }

        private fun isGamCampaignLink(uri: Uri): Boolean {
            val host = uri.host?.lowercase(Locale.US) ?: return false
            val isDoubleClickHost = host == "doubleclick.net" || host.endsWith(".doubleclick.net")
            if (!isDoubleClickHost) {
                return false
            }

            val hasLandingParameter = uri.queryParameterNames.any {
                gamLandingParameterNames.contains(it.lowercase(Locale.US))
            }
            val looksLikeClickPath = uri.path.orEmpty().lowercase(Locale.US).contains("click")
            return hasLandingParameter || looksLikeClickPath
        }

        private fun ofGamCampaignLink(uri: Uri): WvUrlEvent {
            val landingUri = gamLandingParameterNames
                .asSequence()
                .mapNotNull { key -> uri.getQueryParameter(key) }
                .mapNotNull { value ->
                    runCatching {
                        Uri.parse(value).takeIf { parsed ->
                            parsed.scheme.equals("http", ignoreCase = true) ||
                                parsed.scheme.equals("https", ignoreCase = true)
                        }
                    }.getOrNull()
                }
                .firstOrNull()
            val landingCcode = landingUri?.getQueryParameter("ccode")
            val ccode = sanitizedCampaignValue(uri.getQueryParameter("ccode"))
                ?: sanitizedCampaignValue(landingCcode)

            if (ccode == null) {
                Log.i(
                    WEB_PURCHASE_FLOW_TAG,
                    "gam_campaign_external_without_ccode host=${uri.host.orEmpty()} " +
                        "url=${debugWebUrl(uri)}"
                )
                return External(uri)
            }

            Log.i(
                WEB_PURCHASE_FLOW_TAG,
                "gam_campaign_detected host=${uri.host.orEmpty()} " +
                    "ccode=$ccode landing=${debugWebUrl(landingUri)} " +
                    "tracking=webview"
            )
            return CampaignAd(
                uri = uri,
                landingUri = landingUri,
                ccode = ccode,
            )
        }

        private fun ofSubscribe(uri: Uri): WvUrlEvent {
            val tier = Tier.fromString(uri.host?.lowercase(Locale.US))
            val ccode = sanitizedCampaignValue(uri.getQueryParameter("ccode"))
            val from = sanitizedCampaignValue(uri.getQueryParameter("from"))
            val offerHint = sanitizedOfferValue(uri.getQueryParameter("offer"))
            val priceHint = sanitizedPriceHintValue(uri.lastPathSegment)

            Log.i(
                WEB_PURCHASE_FLOW_TAG,
                "parse_subscribe tier=${tier?.symbol.orEmpty()} " +
                    "ccode=${ccode.orEmpty()} from=${from.orEmpty()} " +
                    "offer=${offerHint.orEmpty()} priceHint=${priceHint.orEmpty()} " +
                    "url=${debugWebUrl(uri)}"
            )

            return Subscribe(
                uri = uri,
                tier = tier,
                ccode = ccode,
                from = from,
                offerHint = offerHint,
                priceHint = priceHint,
                sourceScheme = uri.scheme?.lowercase(Locale.US) ?: "subscribe"
            )
        }

        private fun ofWebSubscription(uri: Uri): WvUrlEvent {
            val rawTap = uri.getQueryParameter("tap")
            val tier = tierFromSubscriptionTap(rawTap)
            val ccode = sanitizedCampaignValue(uri.getQueryParameter("ccode"))
            val from = sanitizedCampaignValue(uri.getQueryParameter("from"))
            val offerHint = sanitizedOfferValue(uri.getQueryParameter("offer"))
            val hasIgnoredPrice = !uri.getQueryParameter("price").isNullOrBlank()

            Log.i(
                WEB_PURCHASE_FLOW_TAG,
                "parse_web_subscription tier=${tier?.symbol.orEmpty()} " +
                    "tap=${rawTap.orEmpty()} ccode=${ccode.orEmpty()} " +
                    "from=${from.orEmpty()} offer=${offerHint.orEmpty()} " +
                    "ignoredPriceParam=$hasIgnoredPrice url=${debugWebUrl(uri)}"
            )

            return Subscribe(
                uri = uri,
                tier = tier,
                ccode = ccode,
                from = from,
                offerHint = offerHint,
                priceHint = null,
                sourceScheme = "web-subscription"
            )
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
            val path = uri.lastPathSegment ?: ""
            if (path != "subscription.html" && path != "subscription") {
                return UnknownInSite(uri)
            }

            val ccode = sanitizedCampaignValue(uri.getQueryParameter("ccode"))
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

            val from = sanitizedCampaignValue(uri.getQueryParameter("from"))
            Log.i(
                WEB_PURCHASE_FLOW_TAG,
                "parse_fta_subscription ccode=${ccode.orEmpty()} " +
                    "from=${from.orEmpty()} url=${debugWebUrl(uri)}"
            )

            return FtaSubs(
                uri = uri,
                ccode = ccode,
                from = from,
            )
        }

        @JvmStatic
        private fun ofInSiteLink(uri: Uri): WvUrlEvent {
            /*
             * Safe-mode blocks can redirect WebViews to /login or
             * /login/safe_mode with window.location.replace(), so this must be
             * handled at the navigation classification layer rather than only on
             * explicit taps. Keep the current page in the back stack and launch
             * native auth instead of showing the web login form.
             */
            if (isNativeLoginEntry(uri)) {
                return Login
            }

            val pathSegments = uri.pathSegments

            Log.i(
                WEB_PURCHASE_FLOW_TAG,
                "parse_insite host=${uri.host.orEmpty()} path=${uri.path.orEmpty()} " +
                    "segments=$pathSegments url=${debugWebUrl(uri)}"
            )

            Log.i(TAG, "Pagination Debug: Handle in-site link. uri: $uri")

            Log.i(TAG, "Pagination Debug: Handle in-site link. Path segments: $pathSegments")

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


            Log.i(TAG, "Pagination Debug: Handling URL: $uri")
            Log.i(TAG, "Pagination Debug: Page Number: $pageNumber")


            if (pageNumber != null) {
                Log.i(TAG, "Pagination Debug: Open channel pagination for uri: $uri")

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



            if (isCorpPreview(uri)) {
                Log.i(
                    WEB_PURCHASE_FLOW_TAG,
                    "match_corp_preview route=Channel url=${debugWebUrl(uri)}"
                )
                return Channel(marketingChannelFromUri(uri))
            }

            if (isCorpPage(uri)) {
                val corpUri = withWebviewParam(uri)
                Log.i(
                    WEB_PURCHASE_FLOW_TAG,
                    "match_corp_page route=CorpPage input=${debugWebUrl(uri)} " +
                        "output=${debugWebUrl(corpUri)}"
                )
                return CorpPage(corpUri)
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
                ArticleKind.interactive,
                ArticleKind.content -> Article(teaserFromUri(uri))

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

        private fun isCorpPreview(uri: Uri): Boolean {
            return uri.path == "/m/corp/preview.html" &&
                !uri.getQueryParameter("pageid").isNullOrBlank()
        }

        private fun isCorpPage(uri: Uri): Boolean {
            val pathSegments = uri.pathSegments
            return pathSegments.size == 3 &&
                pathSegments[0] == "m" &&
                pathSegments[1] == "corp" &&
                pathSegments[2].endsWith(".html")
        }

        private fun isWebSubscriptionEntry(uri: Uri): Boolean {
            if (!isSubscriptionPath(uri)) {
                return false
            }

            /*
             * ftcoffer currently renders Android campaign CTAs as relative web
             * links such as:
             * /subscription.html?from=ft_discount&ccode=...&tap=premium#no_universal_links
             *
             * The query tells us only how to enter the native paywall: tier
             * comes from tap, attribution comes from ccode, and discount lookup
             * comes from from/offer. Price is intentionally ignored here because
             * the native checkout must use server-authoritative catalog/payment
             * data instead of trusting a mutable URL parameter.
             */
            return tierFromSubscriptionTap(uri.getQueryParameter("tap")) != null
        }

        private fun isSubscriptionPath(uri: Uri): Boolean {
            val path = uri.path?.trimEnd('/')?.lowercase(Locale.US) ?: return false
            return path == "/subscription.html" || path == "/subscription"
        }

        private fun tierFromSubscriptionTap(tap: String?): Tier? {
            return when (tap?.trim()?.lowercase(Locale.US)) {
                "premium" -> Tier.PREMIUM
                "standard",
                "member",
                "monthly",
                "standardmonthly" -> Tier.STANDARD
                else -> null
            }
        }

        private fun withWebviewParam(uri: Uri): Uri {
            if (!uri.getQueryParameter("webview").isNullOrBlank()) {
                return uri
            }

            return uri.buildUpon()
                .appendQueryParameter("webview", "ftcapp")
                .build()
        }
    }
}

fun WvUrlEvent.debugRouteName(): String {
    return when (this) {
        is WvUrlEvent.MailTo -> "MailTo"
        is WvUrlEvent.Login -> "Login"
        is WvUrlEvent.WxMiniProgram -> "WxMiniProgram"
        is WvUrlEvent.Article -> "Article"
        is WvUrlEvent.Channel -> "Channel"
        is WvUrlEvent.Pagination -> "Pagination"
        is WvUrlEvent.CorpPage -> "CorpPage"
        is WvUrlEvent.UnknownInSite -> "UnknownInSite"
        is WvUrlEvent.FtaSubs -> "FtaSubs"
        is WvUrlEvent.Subscribe -> "Subscribe"
        is WvUrlEvent.External -> "External"
        is WvUrlEvent.CampaignAd -> "CampaignAd"
    }
}

private fun WvUrlEvent.debugRouteDetail(): String {
    return when (this) {
        is WvUrlEvent.CorpPage -> "url=${debugWebUrl(uri)}"
        is WvUrlEvent.UnknownInSite -> "url=${debugWebUrl(uri)}"
        is WvUrlEvent.External -> "url=${debugWebUrl(uri)}"
        is WvUrlEvent.CampaignAd -> "ccode=$ccode landing=${debugWebUrl(landingUri)} " +
            "source=${debugWebUrl(uri)}"
        is WvUrlEvent.Subscribe -> "tier=${tier?.symbol.orEmpty()} " +
            "ccode=${ccode.orEmpty()} from=${from.orEmpty()} " +
            "offer=${offerHint.orEmpty()} priceHint=${priceHint.orEmpty()} " +
            "sourceScheme=$sourceScheme url=${debugWebUrl(uri)}"
        is WvUrlEvent.Channel -> "source=${source.path}?${source.query}"
        is WvUrlEvent.Article -> "teaser=${teaser.type}/${teaser.id}"
        is WvUrlEvent.Pagination -> "key=${paging.key} page=${paging.page}"
        else -> ""
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

    open fun onOverrideUrlLoading(event: WvUrlEvent) {
        Log.i(
            WEB_PURCHASE_FLOW_TAG,
            "dispatch route=${event.debugRouteName()} ${event.debugRouteDetail()}"
        )
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
            is WvUrlEvent.CorpPage -> {
                Log.i(
                    WEB_PURCHASE_FLOW_TAG,
                    "open_corp_webpage url=${debugWebUrl(event.uri)}"
                )
                WebpageActivity.start(
                    context,
                    WebpageMeta(
                        title = "",
                        url = event.uri.toString()
                    )
                )
            }
            is WvUrlEvent.UnknownInSite -> {
                Log.i(
                    WEB_PURCHASE_FLOW_TAG,
                    "open_unknown_insite_webpage url=${debugWebUrl(event.uri)}"
                )
                WebpageActivity.start(
                    context,
                    WebpageMeta(
                        title = "",
                        url = event.uri.toString()
                    )
                )
            }
            is WvUrlEvent.FtaSubs -> {
                val entry = SubscriptionEntryIntent(
                    // FTA subscription pages expose all plans; do not infer a tier or price.
                    tier = null,
                    ccode = event.ccode,
                    from = event.from,
                    sourceUri = event.uri.toString().take(2048),
                    sourceScheme = "fta-subscription",
                )
                Log.i(
                    WEB_PURCHASE_FLOW_TAG,
                    "open_fta_subs_entry ccode=${entry.ccode.orEmpty()} " +
                        "from=${entry.from.orEmpty()} " +
                        "priceHint=${entry.priceHint.orEmpty()} " +
                        "sourceUri=${debugWebUrl(event.uri)}"
                )
                SubsActivity.start(
                    context = context,
                    premiumFirst = false,
                    entry = entry,
                )
            }
            is WvUrlEvent.CampaignAd -> {
                Log.i(
                    WEB_PURCHASE_FLOW_TAG,
                    "open_gam_campaign_webview ccode=${event.ccode} " +
                        "landing=${debugWebUrl(event.landingUri)}"
                )
                WebpageActivity.start(
                    context,
                    WebpageMeta(
                        title = "",
                        url = event.uri.toString(),
                        campaignCode = event.ccode,
                        campaignSourceUrl = event.uri.toString(),
                    )
                )
            }
            is WvUrlEvent.Subscribe -> {
                val entry = SubscriptionEntryIntent(
                    tier = event.tier,
                    ccode = event.ccode,
                    from = event.from,
                    offerHint = event.offerHint,
                    priceHint = event.priceHint,
                    sourceUri = event.uri.toString().take(2048),
                    sourceScheme = event.sourceScheme,
                )
                Log.i(
                    WEB_PURCHASE_FLOW_TAG,
                    "open_subs_activity tier=${entry.tier?.symbol.orEmpty()} " +
                        "premiumFirst=${entry.premiumFirst} ccode=${entry.ccode.orEmpty()} " +
                        "from=${entry.from.orEmpty()} offer=${entry.offerHint.orEmpty()} " +
                        "priceHint=${entry.priceHint.orEmpty()}"
                )
                SubsActivity.start(
                    context = context,
                    premiumFirst = entry.premiumFirst,
                    entry = entry,
                )
            }
            is WvUrlEvent.External -> {
                Log.i(
                    WEB_PURCHASE_FLOW_TAG,
                    "open_external_custom_tabs url=${debugWebUrl(event.uri)}"
                )
                launchCustomTabs(
                    context,
                    event.uri
                )
            }
        }
    }

    fun onOverrideUrlLoading(url: String): Boolean {
        if (url.isBlank()) {
            Log.i(WEB_PURCHASE_FLOW_TAG, "dispatch_string ignored_blank_url")
            return false
        }

        Log.i(WEB_PURCHASE_FLOW_TAG, "dispatch_string url=${debugWebUrl(url)}")
        onOverrideUrlLoading(WvUrlEvent.fromUri(Uri.parse(url)))
        return true
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
        if (!TeaserNavigationGuard.accept(teaser)) {
            Log.i(TAG, "Duplicate story navigation ignored: $teaser")
            return
        }

        ArticleActivity.start(
            context,
            teaser.withParentPerm(channelSource?.permission)
        )
    }

    open fun onLogin() {
        context.startActivity(AuthActivity.newIntent(context))
    }
}

/**
 * Keeps a GAM click wrapper in WebView long enough to record the click, then
 * hands known FTChinese landing routes back to the normal native dispatcher.
 */
class CampaignWebViewCallback(
    context: Context,
    private val sourceUrl: String,
    private val campaignCode: String?,
    private val onResolved: () -> Unit,
) : WebViewCallback(context) {

    private var resolved = false

    override fun onOverrideUrlLoading(event: WvUrlEvent) {
        val route = campaignRoute(event)
        if (route != null) {
            resolve(route)
        } else {
            super.onOverrideUrlLoading(event)
        }
    }

    fun onCampaignPageStarted(view: WebView?, url: String?) {
        if (resolved || url.isNullOrBlank() || url == sourceUrl) {
            return
        }

        val uri = Uri.parse(url)
        val host = uri.host?.lowercase(Locale.US).orEmpty()
        if (host == "doubleclick.net" || host.endsWith(".doubleclick.net")) {
            return
        }

        val route = campaignRoute(WvUrlEvent.fromUri(uri)) ?: return

        resolve(route, view)
    }

    private fun campaignRoute(event: WvUrlEvent): WvUrlEvent? {
        return when (event) {
            is WvUrlEvent.FtaSubs -> event.copy(
                ccode = event.ccode ?: sanitizedCampaignValue(campaignCode)
            )
            is WvUrlEvent.Subscribe -> event.copy(
                ccode = event.ccode ?: sanitizedCampaignValue(campaignCode)
            )
            is WvUrlEvent.Article,
            is WvUrlEvent.Channel,
            is WvUrlEvent.CorpPage -> event
            else -> null
        }
    }

    private fun resolve(route: WvUrlEvent, view: WebView? = null) {
        if (resolved) {
            return
        }
        resolved = true
        view?.stopLoading()
        Log.i(
            WEB_PURCHASE_FLOW_TAG,
            "gam_campaign_landing route=${route.debugRouteName()} " +
                "ccode=${sanitizedCampaignValue(campaignCode).orEmpty()} " +
                "source=${debugWebUrl(sourceUrl)}"
        )
        super.onOverrideUrlLoading(route)
        onResolved()
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
