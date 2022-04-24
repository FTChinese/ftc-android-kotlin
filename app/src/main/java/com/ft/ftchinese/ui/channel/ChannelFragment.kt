package com.ft.ftchinese.ui.channel

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
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
import com.ft.ftchinese.model.content.ChannelContent
import com.ft.ftchinese.model.content.ChannelMeta
import com.ft.ftchinese.model.content.ChannelSource
import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.model.enums.ArticleType
import com.ft.ftchinese.model.fetch.marshaller
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.repository.Config
import com.ft.ftchinese.service.*
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.tracking.SponsorManager
import com.ft.ftchinese.ui.article.ArticleActivity
import com.ft.ftchinese.ui.base.JS_INTERFACE_NAME
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.ui.components.ToastMessage
import com.ft.ftchinese.ui.webpage.ChromeClient
import com.ft.ftchinese.ui.webpage.WVClient
import kotlinx.coroutines.cancel
import kotlinx.serialization.decodeFromString
import org.jetbrains.anko.support.v4.toast
import java.util.*
import kotlin.properties.Delegates

/**
 * Hosted inside [TabPagerAdapter] or [ChannelActivity]
 * when used to handle pagination.
 */
class ChannelFragment : ScopedFragment(),
    SwipeRefreshLayout.OnRefreshListener {

    /**
     * Meta data about current page: the tab's title, where to load data, etc.
     * Passed in when the fragment is created.
     */
    private var channelSource: ChannelSource? = null

    private lateinit var sessionManager: SessionManager
    private lateinit var binding: FragmentChannelBinding

    private lateinit var channelViewModel: ChannelViewModel

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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get metadata about current tab
        channelSource = arguments?.getParcelable(ARG_CHANNEL_SOURCE) ?: return

        start = Date().time / 1000
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        super.onCreateView(inflater, container, savedInstanceState)
        binding = DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_channel,
                container,
                false)

        // Setup swipe refresh listener
        binding.swipeRefresh.setOnRefreshListener(this)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        channelViewModel = ViewModelProvider(this)[ChannelViewModel::class.java]

        // Network status.
        connectionLiveData.observe(viewLifecycleOwner) {
            channelViewModel.isNetworkAvailable.value = it
        }

        setupViewModel()
        setupUI()
        channelSource?.let {
            channelViewModel.load(it, sessionManager.loadAccount())
        }
    }

    private fun setupViewModel() {

        channelViewModel.progressLiveData.observe(viewLifecycleOwner) {
            binding.inProgress = it
        }

        channelViewModel.refreshingLiveData.observe(viewLifecycleOwner) {
            binding.swipeRefresh.isRefreshing = it
            toast(R.string.refresh_success)
        }

        channelViewModel.errorLiveData.observe(viewLifecycleOwner) {
            when (it) {
                is ToastMessage.Resource -> toast(it.id)
                is ToastMessage.Text -> toast(it.text)
            }
        }

        channelViewModel.htmlLiveData.observe(viewLifecycleOwner) {
            load(it)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupUI() {
        // Configure web view.
        binding.webView.settings.apply {
            javaScriptEnabled = true
            loadsImagesAutomatically = true
            domStorageEnabled = true
            databaseEnabled = true
        }

        binding.webView.apply {

            // Interact with JS.
            // See Page/Layouts/Page/SuperDataViewController.swift#viewDidLoad() how iOS inject js to web view.
            addJavascriptInterface(
                this@ChannelFragment,
                JS_INTERFACE_NAME
            )

            // Set WebViewClient to handle various links
            webViewClient = WVClient(
                requireContext(),
                null,
                onEvent = { event ->
                    channelSource?.let {
                        handleWVEvent(
                            context = context,
                            event = event,
                            currentChannel = it
                        )
                    }
                }
            )

            webChromeClient = ChromeClient()

            setOnKeyListener { _, keyCode, _ ->
                if (keyCode == KeyEvent.KEYCODE_BACK && binding.webView.canGoBack()) {
                    binding.webView.goBack()
                    return@setOnKeyListener true
                }

                false
            }
        }
    }

    override fun onRefresh() {
        toast(R.string.refreshing_data)
        channelSource?.let {
            channelViewModel.refresh(it, sessionManager.loadAccount())
        }
    }

    private fun load(html: String) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "Loading web page to web view")
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
     * After HTML is loaded into webview, it will call this
     * method in JS and a list of Teaser is posted.
     */
    @JavascriptInterface
    fun onPageLoaded(message: String) {

        Log.i(TAG, "JS onPageLoaded")

        val channelContent = marshaller.decodeFromString<ChannelContent>(message)

        // Save all teasers.
        articleList = channelContent.sections[0].lists[0].items
        Log.i(TAG, "Channel teasers $articleList")
        channelMeta = channelContent.meta
    }

    @JavascriptInterface
    fun onSelectItem(index: String) {
        Log.i(TAG, "JS select item: $index")

        val i = try {
            index.toInt()
        } catch (e: Exception) {
            -1
        }

        selectItem(i)
    }

    @JavascriptInterface
    fun onLoadedSponsors(message: String) {

        Log.i(TAG, "Loaded sponsors: $message")

        SponsorManager.sponsors = marshaller.decodeFromString(message)
    }

    /**
     * When user clicks on an item of article list,
     * the js interface sends the clickd item index back.
     */
    private fun selectItem(index: Int) {
        Log.i(TAG, "JS interface responding to click on an item")
        if (index < 0) {
            return
        }

        // Find which item user is clicked.
        val teaser = articleList
            ?.getOrNull(index)
            ?.withMeta(channelMeta)
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
        private const val TAG = "ChannelFragment"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        fun newInstance(channel: ChannelSource) = ChannelFragment().apply {
            arguments = bundleOf(ARG_CHANNEL_SOURCE to channel)
        }

    }
}


