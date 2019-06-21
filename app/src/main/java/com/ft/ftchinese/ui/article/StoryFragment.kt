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
import androidx.lifecycle.ViewModelProviders
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ft.ftchinese.R
import com.ft.ftchinese.base.ScopedFragment
import com.ft.ftchinese.base.handleException
import com.ft.ftchinese.base.isNetworkConnected
import com.ft.ftchinese.model.*
import com.ft.ftchinese.ui.ChromeClient
import com.ft.ftchinese.ui.OnProgressListener
import com.ft.ftchinese.ui.channel.JS_INTERFACE_NAME
import com.ft.ftchinese.util.*
import kotlinx.android.synthetic.main.fragment_article.*
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

    private lateinit var cache: FileCache
    private lateinit var followingManager: FollowingManager
    private lateinit var sessionManager: SessionManager
    private lateinit var articleModel: ArticleViewModel
//    private lateinit var starModel: StarArticleViewModel
//    private lateinit var readModel: ReadArticleViewModel

    private var listener: OnProgressListener? = null

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

        if (context is OnProgressListener) {
            listener = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        storyBrief = arguments?.getParcelable(ARG_CHANNEL_ITEM)

        if (storyBrief == null) {
            toast(R.string.prompt_load_failure)
            return
        }

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

        toast(R.string.prompt_refreshing)

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
            ViewModelProviders.of(this).get(ArticleViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        articleModel.currentLang.observe(this, Observer<Language> {
            currentLang = it

            initLoading()
        })

        // If cache is not found, fetch data from remote.
        articleModel.cacheFound.observe(this, Observer {
            if (it) {
                if (activity?.isNetworkConnected() == true) {
                    return@Observer
                }

                val item = storyBrief ?: return@Observer

                // Only load, no rendering.
                articleModel.loadFromRemote(
                        item = item,
                        lang = currentLang,
                        shouldRender = false
                )
                return@Observer
            }

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

        // Observing remote data and cache it.
        articleModel.remoteResult.observe(this, Observer {
            val data = it ?: return@Observer

            val fileName = storyBrief?.cacheNameJson() ?: return@Observer

            articleModel.cacheData(fileName, data)
        })

        // Load html into web view.
        articleModel.renderResult.observe(this, Observer {
            showProgress(false)

            val htmlResult = it ?: return@Observer

            if (htmlResult.exception != null) {
                activity?.handleException(htmlResult.exception)
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

//    private suspend fun loadFromServer() {
//        if (activity?.isNetworkConnected() != true) {
//            showProgress(false)
//            toast(R.string.prompt_no_network)
//            return
//        }
//
//        val url = storyBrief?.buildApiUrl()
//
//        if (url.isNullOrBlank()) {
//            showProgress(false)
//            toast("API endpoint not found")
//            return
//        }
//
//        info("Start fetching data from $url")
//
//        val story = fetchAndCacheRemote()
//
//        if (story == null) {
//            showProgress(false)
//            toast(R.string.api_server_error)
//            return
//        }
//
//        val html = render(story)
//        if (html.isNullOrBlank()) {
//            showProgress(false)
//            toast(R.string.prompt_load_failure)
//            return
//        }
//
//        load(html)
//
//        postLoading(story)
//    }

//    private suspend fun fetchAndCacheRemote(): Story? = withContext(Dispatchers.IO) {
//        val url = storyBrief?.buildApiUrl() ?: return@withContext null
//
//        val data = try {
//            Fetch().get(url).responseString()
//        } catch (e: Exception) {
//            null
//        }
//
//        if (data.isNullOrBlank()) {
//            return@withContext null
//        }
//
//        launch(Dispatchers.IO) {
//            cacheData(data)
//        }
//
//        try {
//            json.parse<Story>(data)
//        } catch (e: Exception) {
//            null
//        }
//    }

//    private suspend fun cacheData(data: String) {
//        val fileName = storyBrief?.cacheNameJson() ?: return
//
//        withContext(Dispatchers.IO) {
//            cache.saveText(fileName, data)
//        }
//    }
//
//    private suspend fun render(story: Story): String? {
//
//        articleModel.showLangSwitcher(story.isBilingual)
//
//        return withContext(Dispatchers.Default) {
//            if (template == null) {
//                template = cache.readStoryTemplate()
//            }
//
//            val follows = followingManager.loadForJS()
//
//            storyBrief?.renderStory(
//                    template = template,
//                    story = story,
//                    language = currentLang,
//                    follows = follows
//            )
//        }
//    }

    private fun load(html: String) {
        web_view.loadDataWithBaseURL(
                FTC_OFFICIAL_URL,
                html,
                "text/html",
                null,
                null)

        showProgress(false)
    }

//    private suspend fun postLoading(story: Story) {
//        // Save reading history.
//        withContext(Dispatchers.IO) {
//            readModel.addOne(story.toReadArticle(storyBrief))
//        }
//
//        // Publish ReadArticle to ViewModel.
//        val article = story.toStarredArticle(storyBrief)
//        info("Publish loaded article. Access right: ${story.accesibleBy}")
//        articleModel.loaded(article)
//        starModel.loaded(article)
//    }

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
