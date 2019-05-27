package com.ft.ftchinese

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.ft.ftchinese.database.StarredArticle
import com.ft.ftchinese.models.*
import com.ft.ftchinese.util.flavorQuery
import com.ft.ftchinese.base.isNetworkConnected
import com.ft.ftchinese.util.json
import com.ft.ftchinese.viewmodel.LoadArticleViewModel
import com.ft.ftchinese.viewmodel.ReadArticleViewModel
import com.ft.ftchinese.viewmodel.StarArticleViewModel
import kotlinx.android.synthetic.main.fragment_web_view.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast

private const val ARG_WEBPAGE_ARTICLE = "arg_web_article"

@kotlinx.coroutines.ExperimentalCoroutinesApi
class WebContentFragment : Fragment(),
        WVClient.OnWebViewInteractionListener,
        AnkoLogger {

    private lateinit var loadModel: LoadArticleViewModel
    private lateinit var starModel: StarArticleViewModel
    private lateinit var readModel: ReadArticleViewModel

    private lateinit var sessionManager: SessionManager

    private var channelItem: ChannelItem? = null

    override fun onOpenGraphEvaluated(result: String) {
        info("Open graph evaluated: $result")

        val og = try {
            json.parse<OpenGraphMeta>(result)
        } catch (e: Exception) {
            null
        }

        info("Open graph evaluation result: $og")

        val article = mergeOpenGraph(og)

        info("Loaded article: $article")

        // Check access here.
        if (!grantAccess(article)) {
            PaywallTracker.fromArticle(article.toChannelItem())

            activity?.finish()
            return
        }

        loadModel.loaded(article)
        starModel.loaded(article)
    }

    /**
     * Refer to [ChannelFragment.selectItem] method
     */
    private fun grantAccess(article: StarredArticle): Boolean {
        if (article.isFree()) {
            info("A free article")
            return true
        }

        val account = sessionManager.loadAccount()

        if (article.requirePremium()) {
            info("Content restricted to premium members")
            return activity?.shouldGrantPremium(account) ?: false
        }


        if (activity?.shouldGrantStandard(account) == false) {
            info("Cannot grant standard access to this article")
            return false
        }

        return activity?.shouldGrantStandard(account) ?: false
    }

    private fun mergeOpenGraph(og: OpenGraphMeta?): StarredArticle {

        return StarredArticle(
                id = if (channelItem?.id.isNullOrBlank()) {
                    og?.extractId()
                } else {
                    channelItem?.id
                } ?: "",
                type = if (channelItem?.type.isNullOrBlank()) {
                    og?.extractType()
                } else {
                    channelItem?.type
                } ?: "",
                subType = channelItem?.subType ?: "",
                title = if (channelItem?.title.isNullOrBlank()) {
                    og?.title
                } else {
                    channelItem?.title
                } ?: "",
                standfirst = og?.description ?: "",
                keywords = channelItem?.tag ?: og?.keywords ?: "",
                imageUrl = og?.image ?: "",
                audioUrl = channelItem?.audioUrl ?: "",
                radioUrl = channelItem?.radioUrl ?: "",
                webUrl = channelItem?.getCanonicalUrl() ?: og?.url ?: "",
                tier =  when {
                    og?.keywords?.contains("会员专享") == true -> Tier.STANDARD.string()
                    og?.keywords?.contains("高端专享") == true -> Tier.PREMIUM.string()
                    else -> ""
                },
                isWebpage = true
        )
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        sessionManager = SessionManager.getInstance(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadModel = activity?.run {
            ViewModelProviders.of(this).get(LoadArticleViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        starModel = activity?.run {
            ViewModelProviders.of(this).get(StarArticleViewModel::class.java)
        } ?: throw java.lang.Exception("Invalid Activity")

        readModel = activity?.run {
            ViewModelProviders.of(this).get(ReadArticleViewModel::class.java)
        } ?: throw Exception("Invalid Activity")


        val itemData = arguments?.getString(ARG_WEBPAGE_ARTICLE)

        if (itemData != null) {
            try {
                channelItem = json.parse<ChannelItem>(itemData)
            } catch (e: Exception) {
                info(e)
            }
        }

        info("Web content source: $channelItem")
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_web_view, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        web_view.settings.apply {
            javaScriptEnabled = true
            loadsImagesAutomatically = true
            domStorageEnabled = true
            databaseEnabled = true
        }

        val wvClient = WVClient(activity)
        wvClient.setWVInteractionListener(this)

        web_view.apply {

            webViewClient = wvClient
            webChromeClient = ChromeClient()

            setOnKeyListener { _, keyCode, _ ->
                if (keyCode == KeyEvent.KEYCODE_BACK && web_view.canGoBack()) {
                    web_view.goBack()
                    return@setOnKeyListener true
                }
                false
            }
        }

        load()
    }

    /**
     * If passed in arg is only a uri, add query paramter to it.
     * If passed in arg is ChannelItem, use its apiUrl. Not need to append query parameter
     */
    private fun buildUrl(): String {
        // Use api webUrl first.
        val apiUrl = channelItem?.buildApiUrl()
        if (!apiUrl.isNullOrBlank()) {
            return apiUrl
        }

        // Fallback to canonical webUrl
        val webUrl = channelItem?.webUrl ?: return ""


        val url = Uri.parse(webUrl)

        val builder = try {
            url.buildUpon()
        } catch (e: Exception) {
            return ""
        }

        if (url.getQueryParameter("webview") == null) {
            builder.appendQueryParameter("webview", "ftcapp")
        }

        val queryValue = flavorQuery[BuildConfig.FLAVOR]

        if (queryValue != null) {
            builder.appendQueryParameter("utm_source", "marketing")
                    .appendQueryParameter("utm_mediu", "androidmarket")
                    .appendQueryParameter("utm_campaign", queryValue)
                    .build()
                    .toString()
        }

        return builder.build().toString()
    }

    private fun load() {
        if (activity?.isNetworkConnected() != true) {
            toast(R.string.prompt_no_network)
            return
        }

        val url = buildUrl()
        info("Load content from: $url")

        web_view.loadUrl(url)

        val article = channelItem?.toStarredArticle() ?: return

        loadModel.loaded(article)
        starModel.loaded(article)
    }

    companion object {
        fun newInstance(channelItem: String) = WebContentFragment().apply {
            arguments = bundleOf(
                    ARG_WEBPAGE_ARTICLE to channelItem
            )
        }
    }
}