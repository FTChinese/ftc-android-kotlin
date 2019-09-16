package com.ft.ftchinese.ui.article

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.ui.base.showException
import com.ft.ftchinese.ui.base.isNetworkConnected
import com.ft.ftchinese.model.*
import com.ft.ftchinese.model.reader.SessionManager
import com.ft.ftchinese.ui.ChromeClient
import com.ft.ftchinese.ui.OnProgressListener
import com.ft.ftchinese.ui.channel.JS_INTERFACE_NAME
import com.ft.ftchinese.util.*
import kotlinx.android.synthetic.main.fragment_article.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast
import java.util.*

private const val ARG_CHANNEL_ITEM = "arg_channel_item"

@kotlinx.coroutines.ExperimentalCoroutinesApi
class StoryFragment : ScopedFragment(),
        SwipeRefreshLayout.OnRefreshListener,
        AnkoLogger {

    private var storyBrief: ChannelItem? = null
    private var currentLang: Language = Language.CHINESE

    private lateinit var cache: FileCache
    private lateinit var followingManager: FollowingManager
    private lateinit var sessionManager: SessionManager
    private lateinit var articleModel: ArticleViewModel

    private var listener: OnProgressListener? = null

    private lateinit var statsTracker: StatsTracker

    private val start = Date().time / 1000

    private fun showProgress(value: Boolean) {
        if (swipe_refresh.isRefreshing) {
            toast(R.string.prompt_updated)
        }

        if (value) {
            listener?.onProgress(true)
        } else {
            listener?.onProgress(false)
            swipe_refresh.isRefreshing = false
        }

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        cache = FileCache(context)
        followingManager = FollowingManager.getInstance(context)
        sessionManager = SessionManager.getInstance(context)
        statsTracker = StatsTracker.getInstance(context)

        if (context is OnProgressListener) {
            listener = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        storyBrief = arguments?.getParcelable(ARG_CHANNEL_ITEM)

        if (storyBrief == null) {
            toast(R.string.loading_failed)
            return
        }

        info("Loading a story: $storyBrief")
        info("onCreate finished")
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_article, container, false)

    override fun onRefresh() {

        val item = storyBrief
        if (item == null) {
            showProgress(false)
            return
        }

        toast(R.string.refreshing_data)

        // Load and render
        articleModel.loadFromRemote(item, currentLang)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipe_refresh.setOnRefreshListener(this)

        web_view.settings.apply {
            javaScriptEnabled = true
            loadsImagesAutomatically = true
            domStorageEnabled = true
            databaseEnabled = true
        }

        val wvClient = WVClient(activity)

        web_view.apply {
            addJavascriptInterface(
                    this@StoryFragment,
                    JS_INTERFACE_NAME
            )

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
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        articleModel = activity?.run {
            ViewModelProvider(
                    this,
                    ArticleViewModelFactory(cache, followingManager))
                    .get(ArticleViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        // Remember the language user selected so
        // that we know how to handle refresh.
        articleModel.currentLang.observe(this, Observer<Language> {
            currentLang = it

            val item = storyBrief ?: return@Observer
            articleModel.loadFromCache(
                    item = item,
                    lang = it
            )
        })

        // If cache is not found, fetch data from remote.
        articleModel.cacheResult.observe(this, Observer {
            if (it.found) {
                return@Observer
            }

            // If cache is not found, fetch data from server.
            if (activity?.isNetworkConnected() != true) {
                toast(R.string.prompt_no_network)
                return@Observer
            }

            val item = storyBrief ?: return@Observer

            // Load and render.
            articleModel.loadFromRemote(
                    item = item,
                    lang = currentLang)
        })

        // Load html into web view.
        articleModel.renderResult.observe(this, Observer {
            showProgress(false)

            val htmlResult = it ?: return@Observer

            if (htmlResult.exception != null) {
                info("Loading html error")
                activity?.showException(htmlResult.exception)
                return@Observer
            }

            if (htmlResult.success.isNullOrBlank()) {
                toast(R.string.api_server_error)
                return@Observer
            }

            load(htmlResult.success)
        })


        initLoading()
    }

    private fun initLoading() {
        val item = storyBrief ?: return

        showProgress(true)

        articleModel.loadFromCache(item, currentLang)
    }

    private fun load(html: String) {
        web_view.loadDataWithBaseURL(
                WV_BASE_URL,
                html,
                "text/html",
                null,
                null)

        showProgress(false)
    }

    override fun onDestroy() {
        super.onDestroy()

        statsTracker.engaged(
                account = sessionManager.loadAccount(),
                start = start,
                end = Date().time / 1000
        )
    }

    @JavascriptInterface
    fun follow(message: String) {
        info("Clicked follow: $message")

        try {
            val following = json.parse<Following>(message) ?: return

            followingManager.save(following)
        } catch (e: Exception) {
            info(e)
        }
    }

    companion object {
        fun newInstance(channelItem: ChannelItem) = StoryFragment().apply {
            arguments = bundleOf(
                    ARG_CHANNEL_ITEM to channelItem
            )
        }
    }
}
