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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentArticleBinding
import com.ft.ftchinese.model.content.*
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.model.reader.ReadingDuration
import com.ft.ftchinese.repository.Config
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.service.ReadingDurationService
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.ui.base.WVClient
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.ui.channel.JS_INTERFACE_NAME
import com.ft.ftchinese.util.*
import com.ft.ftchinese.viewmodel.ArticleViewModel
import com.ft.ftchinese.viewmodel.ArticleViewModelFactory
import com.ft.ftchinese.viewmodel.Result
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast
import java.util.*

private const val ARG_CHANNEL_ITEM = "arg_channel_item"

@kotlinx.coroutines.ExperimentalCoroutinesApi
class StoryFragment : ScopedFragment(),
        SwipeRefreshLayout.OnRefreshListener,
        AnkoLogger {

    private var storyBrief: Teaser? = null
    private var currentLang: Language = Language.CHINESE

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
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_article, container, false)
        return binding.root
    }


    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        articleModel = activity?.run {
            ViewModelProvider(
                    this,
                    ArticleViewModelFactory(cache, sessionManager.loadAccount()))
                    .get(ArticleViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        connectionLiveData.observe(viewLifecycleOwner, {
            articleModel.isNetworkAvailable.value = it
        })
        articleModel.isNetworkAvailable.value = context?.isConnected

        // Observing language switcher.
        articleModel.currentLang.observe(viewLifecycleOwner, Observer {
            // Remember the language user selected so
            // that we know how to handle refresh.
            currentLang = it

            val item = storyBrief ?: return@Observer
            articleModel.loadStory(teaser = item, bustCache = false)
        })

        // Receiving story json data.
        articleModel.storyResult.observe(viewLifecycleOwner, {
            onStoryResult(it)
        })

        initLoading()
    }

    override fun onRefresh() {

        if (context?.isConnected != true) {
            binding.swipeRefresh.isRefreshing = false
            toast(R.string.prompt_no_network)
            return
        }

        val item = storyBrief
        if (item == null) {
            binding.swipeRefresh.isRefreshing = false

            return
        }

        toast(R.string.refreshing_data)

        articleModel.loadStory(
            teaser = item,
            bustCache = true
        )
    }

    private fun initLoading() {
        val item = storyBrief ?: return

        // Show progress indicator
        if (!binding.swipeRefresh.isRefreshing) {
            articleModel.inProgress.value = true
        }

        articleModel.loadStory(
            teaser = item,
            bustCache = false
        )
    }

    private fun onStoryResult(result: Result<Story>) {
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
                val teaser = storyBrief ?: return

                launch {
                    val template = cache.readStoryTemplate()

                    if (template == null) {
                        toast("Error loading data")
                        return@launch
                    }

                    val html = withContext(Dispatchers.Default) {
                        StoryBuilder(template)
                            .setLanguage(currentLang)
                            .withStory(result.data, teaser)
                            .withFollows(followingManager.loadTemplateCtx())
                            .withUserInfo(sessionManager.loadAccount())
                            .render()

                    }

                    load(html)
                }
            }
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

    override fun onDestroy() {
        super.onDestroy()

        val userId = sessionManager.loadAccount()?.id ?: return

        ReadingDurationService.start(context, ReadingDuration(
                url = "/android/${storyBrief?.type}/${storyBrief?.id}/${storyBrief?.title}",
                refer = Config.discoverServer(sessionManager.loadAccount()),
                startUnix = start,
                endUnix = Date().time / 1000,
                userId = userId,
                functionName = "onLoad"
        ))

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
        fun newInstance(channelItem: Teaser) = StoryFragment().apply {
            arguments = bundleOf(
                    ARG_CHANNEL_ITEM to channelItem
            )
        }
    }
}
