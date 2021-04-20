package com.ft.ftchinese.ui.article

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ft.ftchinese.R
import com.ft.ftchinese.database.ArticleDb
import com.ft.ftchinese.databinding.FragmentArticleBinding
import com.ft.ftchinese.model.content.*
import com.ft.ftchinese.model.fetch.json
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.repository.Config
import com.ft.ftchinese.service.*
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.ui.base.WVClient
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.ui.channel.JS_INTERFACE_NAME
import com.ft.ftchinese.viewmodel.Result
import com.google.firebase.messaging.FirebaseMessaging
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast
import java.util.*

@kotlinx.coroutines.ExperimentalCoroutinesApi
class StoryFragment : ScopedFragment(),
        SwipeRefreshLayout.OnRefreshListener,
        AnkoLogger {

    private var teaser: Teaser? = null

    private lateinit var cache: FileCache
    private lateinit var followingManager: FollowingManager
    private lateinit var sessionManager: SessionManager
    private lateinit var articleModel: ArticleViewModel

    private lateinit var statsTracker: StatsTracker
    private lateinit var binding: FragmentArticleBinding

    private val start = Date().time / 1000

    override fun onAttach(context: Context) {
        super.onAttach(context)

        cache = FileCache(context)
        followingManager = FollowingManager.getInstance(context)
        sessionManager = SessionManager.getInstance(context)
        statsTracker = StatsTracker.getInstance(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        connectionLiveData
        teaser = arguments?.getParcelable(ARG_CHANNEL_ITEM)

        if (teaser == null) {
            toast(R.string.loading_failed)
            return
        }

        info("Loading a story: $teaser")
        info("onCreate finished")
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_article, container, false)
        return binding.root
    }


    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        initUI()
        initLoading()
    }

    private fun setupViewModel() {
        articleModel = activity?.run {
            ViewModelProvider(
                this,
                ArticleViewModelFactory(
                    cache,
                    ArticleDb.getInstance(this),
                ),
            ).get(ArticleViewModel::class.java)

        } ?: throw Exception("Invalid Activity")

        connectionLiveData.observe(viewLifecycleOwner, {
            articleModel.isNetworkAvailable.value = it
        })
        articleModel.isNetworkAvailable.value = context?.isConnected

        articleModel.storyLoaded.observe(viewLifecycleOwner) {
            articleModel.compileHtml(followingManager.loadTemplateCtx())
        }

        articleModel.htmlResult.observe(viewLifecycleOwner) { result ->
            articleModel.inProgress.value = false
            binding.swipeRefresh.isRefreshing = false

            when (result) {
                is Result.LocalizedError -> {
                    toast(result.msgId)
                }
                is Result.Error -> {
                    result.exception.message?.let { toast(it) }
                }
                is Result.Success -> {
                    load(result.data)
                }
            }
        }
    }

    private fun initUI() {
        binding.swipeRefresh.setOnRefreshListener(this)

        binding.webView.settings.apply {
            javaScriptEnabled = true
            loadsImagesAutomatically = true
            domStorageEnabled = true
            databaseEnabled = true
        }

        val wvClient = WVClient(requireContext())

        binding.webView.apply {
            addJavascriptInterface(
                this@StoryFragment,
                JS_INTERFACE_NAME
            )

            webViewClient = wvClient
            webChromeClient = WebChromeClient()

            setOnKeyListener { _, keyCode, _ ->
                if (keyCode == KeyEvent.KEYCODE_BACK && binding.webView.canGoBack()) {
                    binding.webView.goBack()
                    return@setOnKeyListener true
                }
                false
            }
        }
    }

    private fun initLoading() {
        if (teaser == null) {
            return
        }

        // Show progress indicator
        if (!binding.swipeRefresh.isRefreshing) {
            articleModel.inProgress.value = true
        }

        teaser?.let {
            articleModel.loadStory(
                teaser = it,
                bustCache = false,
            )
        }
    }

    private fun load(html: String) {
        binding.webView.loadDataWithBaseURL(
                Config.discoverServer(sessionManager.loadAccount()),
                html,
                "text/html",
                null,
                null)

        if (binding.swipeRefresh.isRefreshing) {
            binding.swipeRefresh.isRefreshing = false
            toast(R.string.prompt_updated)
        } else {
            articleModel.inProgress.value = false
        }
    }

    override fun onRefresh() {

        if (teaser == null) {
            binding.swipeRefresh.isRefreshing = false

            return
        }

        toast(R.string.refreshing_data)

        teaser?.let {
            articleModel.loadStory(
                teaser = it,
                bustCache = true,
            )
        }
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
            KEY_DUR_URL to "/android/${teaser?.type}/${teaser?.id}/${teaser?.title}",
            KEY_DUR_REFER to Config.discoverServer(account),
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

    @JavascriptInterface
    fun follow(message: String) {
        info("Clicked follow: $message")

        try {
            val following = json.parse<Following>(message) ?: return

            val isSubscribed = followingManager.save(following)

            if (isSubscribed) {
                FirebaseMessaging.getInstance()
                        .subscribeToTopic(following.topic)
                        .addOnCompleteListener { task ->
                            if (!task.isSuccessful) {
                                info("Subscribing to topic ${following.topic} failed")
                            } else {
                               info("Subscribing to topic ${following.topic} succeeded")
                            }
                        }
            } else {
                FirebaseMessaging.getInstance()
                        .unsubscribeFromTopic(following.topic)
                        .addOnCompleteListener { task ->
                            if (!task.isSuccessful) {
                                info("Unsubscribing from topic ${following.topic} failed")
                            } else {
                                info("Unsubscribing from topic ${following.topic} succeeded")
                            }
                        }
            }
        } catch (e: Exception) {
            info(e)
        }
    }

    companion object {
        private const val ARG_CHANNEL_ITEM = "arg_channel_item"

        @JvmStatic
        fun newInstance(channelItem: Teaser) = StoryFragment().apply {
            arguments = bundleOf(
                    ARG_CHANNEL_ITEM to channelItem
            )
        }
    }
}
