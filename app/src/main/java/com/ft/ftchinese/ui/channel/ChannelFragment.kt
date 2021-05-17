package com.ft.ftchinese.ui.channel

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentChannelBinding
import com.ft.ftchinese.model.content.*
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.fetch.json
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.repository.Config
import com.ft.ftchinese.service.*
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.tracking.SponsorManager
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.ui.ChromeClient
import com.ft.ftchinese.ui.article.ArticleActivity
import com.ft.ftchinese.ui.base.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast
import java.util.*
import kotlin.properties.Delegates

const val JS_INTERFACE_NAME = "Android"

/**
 * Hosted inside [TabPagerAdapter] or [ChannelActivity]
 * when used to handle pagination.
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class ChannelFragment : ScopedFragment(),
    SwipeRefreshLayout.OnRefreshListener,
    AnkoLogger {

    /**
     * Meta data about current page: the tab's title, where to load data, etc.
     * Passed in when the fragment is created.
     */
    private var channelSource: ChannelSource? = null

    private lateinit var sessionManager: SessionManager
    private lateinit var cache: FileCache
    private lateinit var statsTracker: StatsTracker
    private lateinit var binding: FragmentChannelBinding

    private lateinit var channelViewModel: ChannelViewModel
    private lateinit var wvViewModel: WVViewModel

    // An array of article teaser passed from JS.
    // This is used to determine which article user is trying to read.
    private var articleList: List<Teaser>? = null
    private var channelMeta: ChannelMeta? = null
    // Record when this page starts to load.
    private var start by Delegates.notNull<Long>()

    /**
     * Bind listeners here.
     */
    override fun onAttach(context: Context) {
        super.onAttach(context)

        sessionManager = SessionManager.getInstance(context)
        cache = FileCache(context)
        statsTracker = StatsTracker.getInstance(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get metadata about current tab
        channelSource = arguments?.getParcelable(ARG_CHANNEL_SOURCE) ?: return

        start = Date().time / 1000
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        super.onCreateView(inflater, container, savedInstanceState)
        binding = DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_channel,
                container,
                false)

        // Setup swipe refresh listener
        binding.swipeRefresh.setOnRefreshListener(this)

        // Configure web view.
        binding.webView.settings.apply {
            javaScriptEnabled = true
            loadsImagesAutomatically = true
            domStorageEnabled = true
            databaseEnabled = true
        }

        // Setup back key behavior.
        binding.webView.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK && binding.webView.canGoBack()) {
                binding.webView.goBack()
                return@setOnKeyListener true
            }

            false
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        channelViewModel = ViewModelProvider(this, ChannelViewModelFactory(cache, sessionManager.loadAccount()))
            .get(ChannelViewModel::class.java)

        wvViewModel = ViewModelProvider(this)
            .get(WVViewModel::class.java)

        // Network status.
        connectionLiveData.observe(viewLifecycleOwner, {
            channelViewModel.isNetworkAvailable.value = it
        })
        channelViewModel.isNetworkAvailable.value = context?.isConnected

        setupViewModel()
        setupUI()
        initLoading()
    }

    private fun setupViewModel() {

        channelViewModel.progressLiveData.observe(viewLifecycleOwner) {
            binding.inProgress = it
        }

        channelViewModel.swipingLiveData.observe(viewLifecycleOwner) {
            binding.swipeRefresh.isRefreshing = it
            toast(R.string.prompt_updated)
        }

        channelViewModel.htmlRendered.observe(viewLifecycleOwner) {
            info("Loaded channel content: ${channelSource?.fileName}")

            when (it) {
                is FetchResult.LocalizedError -> toast(it.msgId)
                is FetchResult.Error -> it.exception.message?.let { msg -> toast(msg) }
                is FetchResult.Success -> load(it.data)
            }
        }

        /**
         * If user clicked on a link inside webview
         * and the link point to another channel page, open the [ChannelActivity]
         */
        wvViewModel.urlChannelSelected.observe(viewLifecycleOwner, {
            ChannelActivity.start(context, it.withParentPerm(channelSource?.permission))
        })

        // If web view signaled that loading a url is finished.
        wvViewModel.pageFinished.observe(viewLifecycleOwner, {
            // If finished loading, stop progress.
            if (it) {
                binding.inProgress = false
                binding.swipeRefresh.isRefreshing = false
            }
        })

        wvViewModel.pagingBtnClicked.observe(viewLifecycleOwner, {
            onPagination(it)
        })
    }

    private fun setupUI() {
        binding.webView.apply {

            // Interact with JS.
            // See Page/Layouts/Page/SuperDataViewController.swift#viewDidLoad() how iOS inject js to web view.
            addJavascriptInterface(
                this@ChannelFragment,
                JS_INTERFACE_NAME
            )

            // Set WebViewClient to handle various links
            webViewClient = WVClient(requireContext(), wvViewModel)

            webChromeClient = ChromeClient()
        }
    }

    override fun onRefresh() {
        toast(R.string.refreshing_data)
        initLoading()
    }

    private fun initLoading() {
        val chSrc = channelSource
        if (chSrc == null) {
            binding.swipeRefresh.isRefreshing = false
            return
        }

        // For partial HTML, we need to crawl its content and render it with a template file to get the template HTML page.
        when (chSrc.htmlType) {
            HTML_TYPE_FRAGMENT -> {
                info("initLoading: html fragment")

                if (binding.swipeRefresh.isRefreshing) {
                    channelViewModel.refresh(chSrc, sessionManager.loadAccount())
                } else {
                    channelViewModel.load(chSrc, sessionManager.loadAccount())
                }
            }
            // For complete HTML, load it directly into Web view.
            HTML_TYPE_COMPLETE -> {
                val url =  Config.buildChannelSourceUrl(sessionManager.loadAccount(), chSrc) ?: return
                info("initLoading: web page on $url")
                binding.webView.loadUrl(url.toString())
                if (binding.swipeRefresh.isRefreshing) {
                    toast(R.string.prompt_updated)
                    binding.swipeRefresh.isRefreshing = false
                }
            }
        }
    }

    private fun load(html: String) {
        if (BuildConfig.DEBUG) {
            info("Loading web page to web view")
        }
        binding.webView.loadDataWithBaseURL(
            Config.discoverServer(sessionManager.loadAccount()),
            html,
            "text/html",
            null,
            null)
    }

    override fun onPause() {
        super.onPause()
        cancel()
    }

    override fun onStop() {
        super.onStop()
        cancel()
    }

    /**
     * WVClient click pagination.
     */
    private fun onPagination(p: Paging) {
        val source = channelSource ?: return

        val pagedSource = source.withPagination(p.key, p.page)

        info("Open a pagination: $pagedSource")

        // If the the pagination number is not changed, simply refresh it.
        if (pagedSource.shouldReload) {
            onRefresh()
        } else {
            info("Start a new activity for $pagedSource")
            ChannelActivity.start(activity, pagedSource)
        }
    }

    /**
     * Collection data when a web page is loaded into a web view.
     */
    @JavascriptInterface
    fun onPageLoaded(message: String) {

        info("JS onPageLoaded")

        val channelContent = json.parse<ChannelContent>(message) ?: return

        articleList = channelContent.sections[0].lists[0].items
        channelMeta = channelContent.meta

        cacheChannelData(message)
    }

    @JavascriptInterface
    fun onSelectItem(index: String) {
        info("JS select item: $index")

        val i = try {
            index.toInt()
        } catch (e: Exception) {
            -1
        }

        selectItem(i)
    }

    @JavascriptInterface
    fun onLoadedSponsors(message: String) {

//         See what the sponsor data is.
        if (BuildConfig.DEBUG) {
            val name = channelSource?.name

            if (name != null) {
                info("Saving js posted data for sponsors of $channelSource")
                launch {
                    cache.saveText("${name}_sponsors.json", message)
                }
            }
        }

        info("Loaded sponsors: $message")

        try {
            SponsorManager.sponsors = json.parseArray(message) ?: return
        } catch (e: Exception) {
            info(e)
        }
    }

    private fun cacheChannelData(data: String) {
        if (!BuildConfig.DEBUG) {
            return
        }

        val fileName = channelSource?.name ?: return

        launch(Dispatchers.IO) {
            cache.saveText("$fileName.json", data)
        }
    }

    /**
     * When user clicks on an item of article list,
     * the js interface sends the clickd item index back.
     */
    private fun selectItem(index: Int) {
        info("JS interface responding to click on an item")
        if (index < 0) {
            return
        }

        // Find which item user is clicked.
        val teaser = articleList
            ?.getOrNull(index)
            ?.withMeta(channelMeta)
            ?.withParentPerm(channelSource?.permission)
            ?: return

        info("Select item: $teaser")

        /**
         * {
         * "id": "007000049",
         * "type": "column",
         * "headline": "徐瑾经济人" }
         * Canonical URL: http://www.ftchinese.com/channel/column.html
         * Content URL: https://api003.ftmailbox.com/column/007000049?webview=ftcapp&bodyonly=yes
         */
        if (teaser.type == ArticleType.Column) {
            info("Open a column: $teaser")

            ChannelActivity.start(context, ChannelSource.fromTeaser(teaser))
            return
        }

        ArticleActivity.start(activity, teaser)
    }

    override fun onDestroy() {
        super.onDestroy()

        val account = sessionManager.loadAccount() ?: return

        if (account.id == "") {
            return
        }

        sendReadLen(account)
    }

    private fun sendReadLen(account: Account) {
        val data: Data = workDataOf(
            KEY_DUR_URL to "/android/channel/${channelSource?.title}",
            KEY_DUR_REFER to "http://www.ftchinese.com/",
            KEY_DUR_START to start,
            KEY_DUR_END to Date().time / 1000,
            KEY_DUR_USER_ID to account.id
        )

        val lenWorker = OneTimeWorkRequestBuilder<ReadingDurationWorker>()
            .setInputData(data)
            .build()

        context?.run {
            WorkManager.getInstance(this).enqueue(lenWorker)
        }

    }

    companion object {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private const val ARG_CHANNEL_SOURCE = "arg_channel_source"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        fun newInstance(channel: ChannelSource) = ChannelFragment().apply {
            arguments = bundleOf(ARG_CHANNEL_SOURCE to channel)
        }

    }
}


