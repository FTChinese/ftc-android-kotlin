package com.ft.ftchinese.ui.channel

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.ft.ftchinese.model.content.ChannelSource
import com.ft.ftchinese.model.content.OpenGraphMeta
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.repository.Config
import com.ft.ftchinese.service.*
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.Paging
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.ui.base.ToastMessage
import com.ft.ftchinese.ui.web.*
import com.ft.ftchinese.ui.webpage.WVClient
import com.ft.ftchinese.ui.webpage.configWebView
import kotlinx.coroutines.cancel
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

    private val clientListener = object : WebViewListener {
        override fun onOpenGraph(openGraph: OpenGraphMeta) {

        }

        override fun onChannelSelected(source: ChannelSource) {
            ChannelActivity.start(
                context,
                source.withParentPerm(
                    channelSource?.permission
                )
            )
        }

        override fun onPagination(paging: Paging) {
            val currentChannel = channelSource ?: return

            val pagedSource = currentChannel
                .withPagination(paging.key, paging.page)

            if (currentChannel.isSamePage(pagedSource)) {
                return
            }

            ChannelActivity.start(context, pagedSource)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        channelViewModel = ViewModelProvider(this)[ChannelViewModel::class.java]

        // Network status.
        connectionLiveData.observe(viewLifecycleOwner) {
            channelViewModel.isNetworkAvailable.value = it
        }

        setupViewModel()

        configWebView(
            webView = binding.webView,
            jsInterface = JsInterface(
                BaseJsEventListener(
                    context = requireContext(),
                    channelSource = channelSource,
                )
            ),
            client = WVClient(
                context = requireContext(),
                listener = clientListener
            )
        )
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


