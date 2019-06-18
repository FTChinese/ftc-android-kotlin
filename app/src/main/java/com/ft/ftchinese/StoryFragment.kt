package com.ft.ftchinese

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ft.ftchinese.base.ScopedFragment
import com.ft.ftchinese.base.isNetworkConnected
import com.ft.ftchinese.model.*
import com.ft.ftchinese.ui.OnProgressListener
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

@kotlinx.coroutines.ExperimentalCoroutinesApi
class StoryFragment : ScopedFragment(),
        SwipeRefreshLayout.OnRefreshListener,
        AnkoLogger {

    private var storyBrief: ChannelItem? = null
    private var currentLang: Language = Language.CHINESE

    private var template: String? = null

    private var loadingJob: Job? = null

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

//            loadContent()
            initLoading()
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

    override fun onRefresh() {

        if (loadingJob?.isActive == true) {
            loadingJob?.cancel()
        }

        toast(R.string.prompt_refreshing)
        launch {
            loadFromServer()
            toast(R.string.prompt_updated)
        }
    }

    private fun showProgress(value: Boolean) {
        if (value) {
            listener?.onProgress(true)
        } else {
            listener?.onProgress(false)
            swipe_refresh.isRefreshing = false
        }
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

//        loadContent()

        initLoading()
    }

    private fun initLoading() {
        loadingJob = launch {
            val cachedStory = loadFromCache()
            if (cachedStory != null) {
                val html = render(cachedStory)
                if (html != null) {
                    load(html)
                    postLoading(cachedStory)

                    if (activity?.isNetworkConnected() == true) {
                        fetchAndCacheRemote()
                    }

                    return@launch
                }

                loadFromServer()
            }

            loadFromServer()
        }
    }

    private suspend fun loadFromCache(): Story? {
        val cacheName = storyBrief?.cacheNameJson()
        info("Cache file name: $cacheName")

        if (cacheName.isNullOrBlank()) {
            info("No cache file name")
            return null
        }

       return withContext(Dispatchers.IO) {
            val data = cache.loadText(cacheName) ?: return@withContext  null

            try {
                json.parse<Story>(data)
            } catch (e: Exception) {
                null
            }
        }
    }

    private suspend fun loadFromServer() {
        if (activity?.isNetworkConnected() != true) {
            showProgress(false)
            toast(R.string.prompt_no_network)
            return
        }

        val url = storyBrief?.buildApiUrl()

        if (url.isNullOrBlank()) {
            showProgress(false)
            toast("API endpoint not found")
            return
        }

        info("Start fetching data from $url")

        val story = fetchAndCacheRemote()

        if (story == null) {
            showProgress(false)
            toast(R.string.api_server_error)
            return
        }

        val html = render(story)
        if (html.isNullOrBlank()) {
            showProgress(false)
            toast(R.string.prompt_load_failure)
            return
        }

        load(html)

        postLoading(story)
    }

    private suspend fun fetchAndCacheRemote(): Story? = withContext(Dispatchers.IO) {
        val url = storyBrief?.buildApiUrl() ?: return@withContext null

        val data = try {
            Fetch().get(url).responseString()
        } catch (e: Exception) {
            null
        }

        if (data.isNullOrBlank()) {
            return@withContext null
        }

        launch(Dispatchers.IO) {
            cacheData(data)
        }

        try {
            json.parse<Story>(data)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun cacheData(data: String) {
        val fileName = storyBrief?.cacheNameJson() ?: return

        withContext(Dispatchers.IO) {
            cache.saveText(fileName, data)
        }
    }

    private suspend fun render(story: Story): String? {

        // After story data is loaded, perform permission
        // check again.
//        val account = sessionManager.loadAccount()
//        val granted = activity?.grantPermission(account, story.permission())
//
//        if (granted == false) {
//            toast("Incorrect membership to access this article")
//            cancel()
//
//            activity?.finish()
//            return null
//        }

        loadModel.showLangSwitcher(story.isBilingual)

        return withContext(Dispatchers.Default) {
            if (template == null) {
                template = cache.readStoryTemplate()
            }

            val follows = followingManager.loadForJS()

            storyBrief?.renderStory(
                    template = template,
                    story = story,
                    language = currentLang,
                    follows = follows
            )
        }
    }

    private fun load(html: String) {
        web_view.loadDataWithBaseURL(
                FTC_OFFICIAL_URL,
                html,
                "text/html",
                null,
                null)

        showProgress(false)
    }

    private suspend fun postLoading(story: Story) {
        // Save reading history.
        withContext(Dispatchers.IO) {
            readModel.addOne(story.toReadArticle(storyBrief))
        }

        // Publish ReadArticle to ViewModel.
        val article = story.toStarredArticle(storyBrief)
        info("Publish loaded article. Access right: ${story.accesibleBy}")
        loadModel.loaded(article)
        starModel.loaded(article)
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
