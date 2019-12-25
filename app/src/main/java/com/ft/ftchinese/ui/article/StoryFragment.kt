package com.ft.ftchinese.ui.article

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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentArticleBinding
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.ui.base.isNetworkConnected
import com.ft.ftchinese.model.*
import com.ft.ftchinese.model.reader.ReadingDuration
import com.ft.ftchinese.model.reader.SessionManager
import com.ft.ftchinese.service.ReadingDurationService
import com.ft.ftchinese.ui.ChromeClient
import com.ft.ftchinese.ui.channel.JS_INTERFACE_NAME
import com.ft.ftchinese.util.*
import com.ft.ftchinese.viewmodel.ArticleViewModel
import com.ft.ftchinese.viewmodel.ArticleViewModelFactory
import com.ft.ftchinese.viewmodel.Result
import com.google.firebase.messaging.FirebaseMessaging
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

//    private var listener: OnProgressListener? = null

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
        articleModel.currentLang.observe(viewLifecycleOwner, Observer<Language> {
            currentLang = it

            val item = storyBrief ?: return@Observer
            articleModel.loadFromCache(
                    item = item,
                    lang = it
            )
        })

        // If cache is not found, fetch data from remote.
        articleModel.cacheFound.observe(viewLifecycleOwner, Observer {
            onCacheFound(it)
        })

        // Load html into web view.
        articleModel.renderResult.observe(viewLifecycleOwner, Observer {
            onRenderResult(it)
        })

        initLoading()
    }

    override fun onRefresh() {

        val item = storyBrief
        if (item == null) {
            binding.swipeRefresh.isRefreshing = false
//            showProgress(false)

            return
        }

        toast(R.string.refreshing_data)

        // Load and render
        articleModel.loadFromRemote(item, currentLang)
    }

    private fun initLoading() {
        val item = storyBrief ?: return

        if (!binding.swipeRefresh.isRefreshing) {
            articleModel.inProgress.value = true
        }

        articleModel.loadFromCache(item, currentLang)
    }

    private fun onCacheFound(found: Boolean) {
        if (found) {
            return
        }

        // If cache is not found, fetch data from server.
        if (activity?.isNetworkConnected() != true) {
            toast(R.string.prompt_no_network)
            return
        }

        val item = storyBrief ?: return

        // Load and render.
        articleModel.loadFromRemote(
                item = item,
                lang = currentLang)
    }

    private fun onRenderResult(result: Result<String>) {
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

    private fun load(html: String) {
        binding.webView.loadDataWithBaseURL(
                BASE_URL,
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
                url = "http://www.ftchinese.com/",
                refer = "http://www.ftchinese.com/",
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
