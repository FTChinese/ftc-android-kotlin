package com.ft.ftchinese.ui.webpage

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.model.content.*
import com.ft.ftchinese.model.enums.ArticleType
import com.ft.ftchinese.model.fetch.marshaller
import com.ft.ftchinese.model.legal.WebpageMeta
import com.ft.ftchinese.repository.Config
import com.ft.ftchinese.tracking.*
import com.ft.ftchinese.ui.SubsActivity
import com.ft.ftchinese.ui.article.ArticleActivity
import com.ft.ftchinese.ui.base.*
import com.ft.ftchinese.ui.channel.ChannelActivity
import com.ft.ftchinese.ui.dialog.AlertDialogFragment
import com.ft.ftchinese.ui.login.AuthActivity
import com.ft.ftchinese.ui.share.ScreenshotViewModel
import com.ft.ftchinese.ui.share.ShareUtils
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.serialization.decodeFromString
import org.jetbrains.anko.toast

private const val TAG =  "WVBaseFragment"

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
 * A base fragment collects shared functionalities for webview-based page.
 */
abstract class WVBaseFragment : ScopedFragment(), WVClient.Listener {

    protected lateinit var wvViewModel: WVViewModel
    protected lateinit var screenshotViewModel: ScreenshotViewModel
    private lateinit var followingManager: FollowingManager

    private var teasers: List<Teaser>? = null
    /**
     * Meta data about current page: the tab's title, where to load data, etc.
     * Passed in when the fragment is created.
     */
    protected var channelSource: ChannelSource? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        followingManager = FollowingManager.getInstance(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        wvViewModel = activity?.run {
            ViewModelProvider(this)[WVViewModel::class.java]
        } ?: throw Exception("Invalid activity")

        screenshotViewModel = activity?.run {
            ViewModelProvider(this)[ScreenshotViewModel::class.java]
        } ?: throw Exception("Invalid Activity")
    }

    @SuppressLint("SetJavaScriptEnabled")
    protected fun configWebView(wv: WebView) {
        wv.settings.apply {
            javaScriptEnabled = true
            loadsImagesAutomatically = true
            domStorageEnabled = true
            databaseEnabled = true
        }

        wv.apply {
            addJavascriptInterface(
                this@WVBaseFragment,
                JS_INTERFACE_NAME
            )

            webViewClient = WVClient(
                this@WVBaseFragment
            )

            webChromeClient = ChromeClient()

            setOnKeyListener { _, keyCode, _ ->
                if (keyCode == KeyEvent.KEYCODE_BACK && wv.canGoBack()) {
                    wv.goBack()
                    return@setOnKeyListener true
                }
                false
            }
        }
    }

    protected fun takeScreenshot(wv: WebView, saveTo: Uri): Boolean {
        Log.i(TAG, "Webview width ${wv.width}, height ${wv.height}")

        val bitmap = Bitmap.createBitmap(
            wv.width,
            wv.height,
            Bitmap.Config.ARGB_8888)

        val canvas = Canvas(bitmap)
        Log.i(TAG, "Drawing webview...")
        wv.draw(canvas)

        Log.i(TAG, "Save image to $saveTo")

        return requireContext()
            .contentResolver
            .openOutputStream(saveTo, "w")
            ?.use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, it)

                it.flush()

                bitmap.recycle()
                true
            }
            ?: false
    }

    @JavascriptInterface
    fun follow(message: String) {
        Log.i(TAG, "Clicked follow: $message")
        try {
            val f = marshaller.decodeFromString<Following>(message) ?: return
            val isSubscribed = followingManager.save(f)

            if (isSubscribed) {
                FirebaseMessaging.getInstance()
                    .subscribeToTopic(f.topic)
                    .addOnCompleteListener { task ->
                        Log.i(ArticleActivity.TAG, "Subscribing to topic ${f.topic} success: ${task.isSuccessful}")
                    }
            } else {
                FirebaseMessaging.getInstance()
                    .unsubscribeFromTopic(f.topic)
                    .addOnCompleteListener { task ->
                        Log.i(ArticleActivity.TAG, "Unsubscribing from topic ${f.topic} success: ${task.isSuccessful}")
                    }
            }
        } catch (e: Exception) {
            e.message?.let { Log.i(TAG, it) }
        }
    }

    @JavascriptInterface
    fun wvClosePage() {
        activity?.finish()
    }

    @JavascriptInterface
    fun wvAlert(msg: String) {
        AlertDialogFragment
            .newMsgInstance(msg)
            .show(childFragmentManager, "WVBaseFragment")
    }

    /**
     * Handle ChannelFragment initial loading
     * After HTML is loaded into webview, it will call this
     * method in JS and a list of Teaser is posted.
     */
    @JavascriptInterface
    fun onPageLoaded(message: String) {

        Log.i(TAG, "JS onPageLoaded")

        val channelContent = marshaller.decodeFromString<ChannelContent>(message)

        // Save all teasers.
        val articleList = channelContent.sections[0].lists[0].items
        Log.i(TAG, "Channel teasers $articleList")

        val channelMeta = channelContent.meta

        teasers = articleList.map {
            it.withMeta(channelMeta)
        }

        // TODO: cache message.
    }

    /**
     * Handle click article teaser in a channel page.
     */
    @JavascriptInterface
    fun onSelectItem(index: String) {
        Log.i(TAG, "JS select item: $index")

        val i = try {
            index.toInt()
        } catch (e: Exception) {
            -1
        }

        selectTeaser(i)
    }

    private fun selectTeaser(index: Int) {
        Log.i(TAG, "JS interface responding to click on an item")

        val teaser = teasers?.getOrNull(index)
            ?.withParentPerm(channelSource?.permission)
            ?: return

        Log.i(TAG, "Select item: $teaser")

        /**
         * {
         * "id": "007000049",
         * "type": "column",
         * "headline": "徐瑾经济人" }
         * Canonical URL: http://www.ftchinese.com/channel/column.html
         * Content URL: https://api003.ftmailbox.com/column/007000049?webview=ftcapp&bodyonly=yes
         */
        if (teaser.type == ArticleType.Column) {
            Log.i(TAG, "Open a column: $teaser")

            ChannelActivity.start(context, ChannelSource.fromTeaser(teaser))
            return
        }

        /**
         * For this type of data, load url directly.
         * Teaser(
         * id=44330,
         * type=interactive,
         * subType=mbagym,
         * title=一周新闻小测：2021年07月17日,
         * audioUrl=null,
         * radioUrl=null,
         * publishedAt=null,
         * tag=FT商学院,教程,一周新闻,入门级,FTQuiz,AITranslation)
         */
        ArticleActivity.start(activity, teaser)
    }

    @JavascriptInterface
    fun onLoadedSponsors(message: String) {

        Log.i(TAG, "Loaded sponsors: $message")

        marshaller.decodeFromString<List<Sponsor>>(message).let {
            SponsorManager.sponsors = it
        }
    }

    override fun onOverrideURL(uri: Uri): Boolean {
        if (ShareUtils.containWxMiniProgram(uri)) {
            val params = ShareUtils.wxMiniProgramParams(uri)
            if (params != null) {
                Log.i(TAG, "Open in wechat mini program $params")
                ShareUtils
                    .createWxApi(requireContext())
                    .sendReq(
                        ShareUtils.wxMiniProgramReq(params)
                    )
                StatsTracker
                    .getInstance(requireContext())
                    .openedInWxMini(params)
                return true
            }
            // otherwise fallthrough.
        }

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
                AuthActivity.start(requireContext())
                return true
            }

            "ftchinese" -> {
                if (uri.pathSegments.size > 0 && uri.pathSegments[0] != TYPE_STORY) {
                    ArticleActivity.start(context, teaserFromFtcSchema(uri))
                } else {
                    context?.toast("Unsupported link!")
                }
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

    override fun onPageFinished(url: String?) {
        wvViewModel.pageFinished.value = true
    }

    override fun onOpenGraph(og: String) {
        wvViewModel.openGraphEvaluated.value = og
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

            SubsActivity.start(context = context)
        }

        return true
    }

    private fun handleInSiteLink(uri: Uri): Boolean {

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
//            mListener?.onPagination(paging)

            onPagination(paging)
            return true
        }

        // In case no path segments
        if (pathSegments.size == 0) {
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

                ArticleActivity.start(context, teaserFromUri(uri))
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
                Log.i(TAG, "Open a channel link: $uri")
                onChannelUrlSelected(channelFromUri(uri))
                true
            }

            /**
             * This kind of page is a list of articles
             * If the path looks like `/m/marketing/intelligence.html`
             * or /m/corp/preview.html?pageid=huawei2018
             */
            TYPE_M -> {
                Log.i(TAG, "Loading marketing page")

                onChannelUrlSelected(marketingChannelFromUri(uri))

                true
            }

            /**
             * If the path looks like `/tag/中美贸易战`, `/archiver/2019-03-05`
             * start a new page listing articles
             */
            TYPE_TAG,
            TYPE_ARCHIVE -> {
                Log.i(TAG, "Loading tag or archive")
                onChannelUrlSelected(tagOrArchiveChannel(uri))
                true
            }

            else -> {
                Log.i(TAG, "Loading a plain web page")
                WebpageActivity.start(
                    requireContext(),
                    WebpageMeta(
                        title = "",
                        url = uri.toString()
                    )
                )
                true
            }
        }
    }

    private fun onChannelUrlSelected(source: ChannelSource) {
        ChannelActivity.start(
            context,
            source.withParentPerm(channelSource?.permission)
        )
    }

    private fun feedbackEmail(): Boolean {
        val pm = context?.packageManager ?: return true

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, "ftchinese.feedback@gmail.com")
            putExtra(Intent.EXTRA_SUBJECT, "Feedback on FTC Android App")
        }

        return if (intent.resolveActivity(pm) != null) {
            context?.startActivity(intent)
            true
        } else {
            context?.toast(R.string.prompt_no_email_app)
            true
        }
    }

    private fun handleExternalLink(uri: Uri): Boolean {
        UrlHandler.openInCustomTabs(requireContext(), uri)

        return true
    }

    /**
     * WVClient click pagination.
     */
    private fun onPagination(p: Paging) {
        val source = channelSource ?: return

        val pagedSource = source.withPagination(p.key, p.page)

        Log.i(TAG, "Open a pagination: $pagedSource")

        // If the the pagination number is not changed, simply refresh it.
        if (pagedSource.shouldReload) {
            onWebPageRefresh()
        } else {
            Log.i(TAG, "Start a new activity for $pagedSource")
            ChannelActivity.start(activity, pagedSource)
        }
    }

    abstract fun onWebPageRefresh()
}
