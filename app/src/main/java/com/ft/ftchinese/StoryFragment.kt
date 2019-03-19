package com.ft.ftchinese

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ft.ftchinese.models.*
import com.ft.ftchinese.user.OnProgressListener
import com.ft.ftchinese.util.*
import com.ft.ftchinese.viewmodel.LoadArticleViewModel
import com.ft.ftchinese.viewmodel.ReadArticleViewModel
import com.ft.ftchinese.viewmodel.StarArticleViewModel
import kotlinx.android.synthetic.main.fragment_article.*
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast

private const val ARG_CHANNEL_ITEM = "arg_channel_item"

class StoryFragment : Fragment(),
        SwipeRefreshLayout.OnRefreshListener,
        AnkoLogger {

    private var storyBrief: ChannelItem? = null
    private var currentLang: Language = Language.CHINESE
    private var job: Job? = null
    private var template: String? = null

    private lateinit var cache: FileCache
    private lateinit var followingManager: FollowingManager
    private lateinit var sessionManager: SessionManager
    private lateinit var loadModel: LoadArticleViewModel
    private lateinit var starModel: StarArticleViewModel
    private lateinit var readModel: ReadArticleViewModel

    private var listener: OnProgressListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        cache = FileCache(context)
        followingManager = FollowingManager.getInstance(context)
        sessionManager = SessionManager.getInstance(context)

        if (context is OnProgressListener) {
            listener = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data = arguments?.getString(ARG_CHANNEL_ITEM)

        info("Story source data: $data")

        if (data == null) {
            toast(R.string.prompt_load_failure)
            return
        }

        // Parse arguments
        storyBrief = json.parse<ChannelItem>(data)
        if (storyBrief == null) {
            toast(R.string.prompt_load_failure)
            return
        }

        loadModel = activity?.run {
            ViewModelProviders.of(this).get(LoadArticleViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        loadModel.currentLang.observe(this, Observer<Language> {
            currentLang = it

            loadContent()
        })

        starModel = activity?.run {
            ViewModelProviders.of(this).get(StarArticleViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        readModel = activity?.run {
            ViewModelProviders.of(this).get(ReadArticleViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        info("onCreate finished")
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_article, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipe_refresh.setOnRefreshListener(this)



        web_view.settings.apply {
            javaScriptEnabled = true
            loadsImagesAutomatically = true
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

        loadContent()
    }


    private fun loadContent() {
        if (storyBrief == null) {
            toast(R.string.prompt_load_failure)
            return
        }

        job = GlobalScope.launch(Dispatchers.Main) {
            val cacheName = storyBrief?.cacheNameJson()
            info("Cache file name: $cacheName")

            if (cacheName.isNullOrBlank()) {
                info("No cache file name")
                loadFromServer()
                return@launch
            }

            val cachedJson = withContext(Dispatchers.IO) {
                cache.loadText(cacheName)
            }

            if (cachedJson.isNullOrBlank()) {
                info("Cache file is not found or is blank")
                loadFromServer()
                return@launch
            }

            if (template == null) {
                template = cache.readStoryTemplate()
            }

            info("Use local cache")

            renderAndLoad(cachedJson)
        }
    }

    private suspend fun loadFromServer() {
        if (activity?.isNetworkConnected() == false) {
            listener?.onProgress(false)
            swipe_refresh.isRefreshing = false

            toast(R.string.prompt_no_network)
            return
        }

        val url = storyBrief?.buildApiUrl() ?: return

        if (!swipe_refresh.isRefreshing) {
            listener?.onProgress(true)
        } else {
            toast(R.string.prompt_refreshing)
        }

        info("Start fetching data from $url")

        try {
            val data = withContext(Dispatchers.IO) {
                Fetch().get(url).responseString()
            }

            // Stop progress indicator
            listener?.onProgress(false)
            swipe_refresh.isRefreshing = false

            // If data is not fetched, stop.
            if (data.isNullOrBlank()) {
                toast(R.string.prompt_load_failure)
                return
            }

            // Render UI.
            renderAndLoad(data)

            // Cache data after retrieved from remote server.
            cacheData(data)
        } catch (e: Exception) {
            info(e)
            toast(R.string.prompt_load_failure)
        }
    }

    private suspend fun cacheData(data: String) {
        val fileName = storyBrief?.cacheNameJson() ?: return

        withContext(Dispatchers.IO) {
            cache.saveText(fileName, data)
        }
    }

    private fun renderAndLoad(data: String) {

        if (template == null) {
            template = cache.readStoryTemplate()
        }

        val story = try {
            json.parse<Story>(data)
        } catch (e: Exception) {
            info(e)
            toast(R.string.prompt_load_failure)
            null
        }

        if (story == null) {
            toast(R.string.prompt_load_failure)
            return
        }

        // Check access here, again.
        if (!grantAccess(story)) {
            activity?.finish()
            return
        }

        loadModel.showLangSwitcher(story.isBilingual)

        // Publish StarredArticle to ViewModel here.
        // Tell host activity whether title bar should be shown.

        val follows = followingManager.loadForJS()
        info("Follow tags: $follows")

        val html = storyBrief?.renderStory(
                template = template,
                story = story,
                language = currentLang,
                follows = follows
        )

        if (html == null) {
            toast(R.string.prompt_load_failure)
            return
        }

        web_view.loadDataWithBaseURL(
                FTC_OFFICIAL_URL,
                html,
                "text/html",
                null,
                null)

        // Save reading history
        GlobalScope.launch(Dispatchers.IO) {
            readModel.addOne(story.toReadArticle(storyBrief))
        }

        // Publish ReadArticle to ViewModel.
        val article = story.toStarredArticle(storyBrief)
        info("Publish loaded article. Access right: ${story.accesibleBy}")
        loadModel.loaded(article)
        starModel.loaded(article)
    }

    private fun grantAccess(story: Story): Boolean {
        if (story.isFree()) {
            info("A free article")
            return true
        }

        val account = sessionManager.loadAccount()

        if (activity?.shouldGrantStandard(account, null) == false) {
            info("Cannot grant standard access to this article")
            return false
        }

        return true
    }

    override fun onRefresh() {

        if (job?.isActive == true) {
            job?.cancel()
        }

        job = GlobalScope.launch(Dispatchers.Main) {
            loadFromServer()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
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
        fun newInstance(channelItem: String) = StoryFragment().apply {
            arguments = bundleOf(
                    ARG_CHANNEL_ITEM to channelItem
            )
        }
    }
}