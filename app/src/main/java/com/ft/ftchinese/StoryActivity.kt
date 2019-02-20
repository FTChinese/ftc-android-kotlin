package com.ft.ftchinese

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.ft.ftchinese.models.*
import com.ft.ftchinese.user.CredentialsActivity
import com.ft.ftchinese.user.SubscriptionActivity
import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.util.isNetworkConnected
import com.ft.ftchinese.util.json
import kotlinx.android.synthetic.main.activity_content.*
import kotlinx.coroutines.*
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

/**
 * StoryActivity is used to show a story whose has a JSON api on server.
 * The remote JSON is fetched and concatenated with local HTML mTemplate in `res/raw/story.html`.
 * For those articles that do not have a JSON api, do not use this activity. Load the a web page directly into web view.
 */
class StoryActivity : AbsContentActivity() {

    override val articleWebUrl: String
        get() = mChannelItem?.canonicalUrl ?: ""

    override val articleTitle: String
        get() = mChannelItem?.headline ?: mChannelItem?.canonicalUrl ?: ""

    override val articleStandfirst: String
        get() = mChannelItem?.standfirst ?: ""

    private var mCurrentLanguage: Int = ChannelItem.LANGUAGE_CN

    // Hold metadata on where and how to find data for this page.
    override var mChannelItem: ChannelItem? = null

    private var mLoadJob: Job? = null
//    private var mCacheJob: Job? = null
    private var mRefreshJob: Job? = null

//    private var mRequest: Request? = null

    private var mTemplate: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        // Meta data about current article
        val itemData = intent.getStringExtra(EXTRA_CHANNEL_ITEM)

        if (itemData == null) {
            toast(R.string.prompt_load_failure)
            return
        }

        mChannelItem = json.parse<ChannelItem>(itemData)

        if (mChannelItem == null) {
            toast(R.string.prompt_load_failure)
            return
        }

        loadContent()
        updateStarUI()
        setup()
    }

    private fun setup() {
        titlebar_cn.setOnClickListener {
            mCurrentLanguage = ChannelItem.LANGUAGE_CN
            loadContent()
        }

        titlebar_en.setOnClickListener {
            val user = mSessionManager?.loadAccount()
            mCurrentLanguage = ChannelItem.LANGUAGE_EN

            if (user == null) {
                showSignIn()
                return@setOnClickListener
            }

            if (!user.canAccessPaidContent) {
                showSubs()
                return@setOnClickListener
            }

            loadContent()
        }

        titlebar_bi.setOnClickListener {
            val user = mSessionManager?.loadAccount()
            mCurrentLanguage = ChannelItem.LANGUAGE_BI

            if (user == null) {
                showSignIn()
                return@setOnClickListener
            }

            if (!user.canAccessPaidContent) {
                showSubs()
                return@setOnClickListener
            }


            loadContent()
        }
    }

    private fun showSignIn() {
        enableLangSwitch(false)

        toast(R.string.prompt_restricted_login)

        CredentialsActivity.startForResult(this)
    }

    private fun showSubs() {
        enableLangSwitch(false)

        toast(R.string.prompt_restricted_paid_user)

        val channelItem = mChannelItem
        if (channelItem == null) {
            SubscriptionActivity.start(this)
            return
        }

        SubscriptionActivity.start(context = this, source = PaywallSource(
                id = channelItem.id,
                category = channelItem.type,
                name = channelItem.headline,
                variant = mCurrentLanguage
        ))
    }

    private fun loadContent() {

        mLoadJob = GlobalScope.launch(Dispatchers.Main) {

            val cacheName = mChannelItem?.cacheFileName
            info("Cache file name: $cacheName")

            if (cacheName.isNullOrBlank()) {
                info("No cache file name")
                loadFromServer()
                return@launch
            }

            val cachedJson = withContext(Dispatchers.IO) {
                mFileCache?.loadText(cacheName)
            }

            if (cachedJson.isNullOrBlank()) {
                info("Cache file is not found or is blank")
                loadFromServer()
                return@launch
            }

            if (mTemplate == null) {
                mTemplate = mFileCache?.readStoryTemplate()
            }

            info("Use local cache")
            renderAndLoad(cachedJson)
        }

        logViewItemEvent()
    }

    private suspend fun loadFromServer() {

        if (!isNetworkConnected()) {
            showProgress(false)
            toast(R.string.prompt_no_network)
            return
        }

        val url = mChannelItem?.apiUrl ?: return

        if (mTemplate == null) {
            mTemplate = mFileCache?.readStoryTemplate()
        }

        if (!swipe_refresh.isRefreshing) {
            showProgress(true)
        } else {
            toast(R.string.prompt_refreshing)
        }

        info("Start fetching data from $url")

        try {
            val data = withContext(Dispatchers.IO) {
                Fetch().get(url).responseString()
            }
            showProgress(false)

            if (data.isNullOrBlank()) {
                return
            }

            renderAndLoad(data)

            cacheData(data)
        } catch (e: Exception) {
            info(e.message)
        }

//        mRequest = Fuel.get(url)
//                .responseString { _, _, result ->
//                    isInProgress = false
//
//                    val (data, error) = result
//
//                    info("Error: $error")
//
//                    if (error != null || data == null) {
//                        toast(R.string.prompt_load_failure)
//                        return@responseString
//                    }
//
//                    info("Start rendering")
//
//                    renderAndLoad(data)
//
//                    cacheData(data)
//                }
    }

    private suspend fun cacheData(data: String) {

        val fileName = mChannelItem?.cacheFileName ?: return

        withContext(Dispatchers.IO) {
            mFileCache?.saveText(fileName, data)
        }
    }

    private fun renderAndLoad(data: String) {

        val story = json.parse<Story>(data)

        if (story == null) {
            toast(R.string.prompt_load_failure)
            return
        }

        mChannelItem?.headline = story.cheadline

        info("Story: $story")

        showLangSwitch(story.isBilingual)

        val follows = mFollowingManager?.loadForJS() ?: JSFollows(mapOf())

        val html = mChannelItem?.renderStory(mTemplate, story, mCurrentLanguage, follows = follows)

        if (html == null) {
            toast(R.string.prompt_load_failure)
            return
        }

//        CookieManager.getInstance().apply {
//            setAcceptCookie(true)
//            setCookie("http://www.ftchinese.com", "username=${user?.name}")
//            setCookie("http://www.ftchinese.com", "userId=${user?.id}")
//            setCookie("http://www.ftchinese.com", "uniqueVisitorId=${user?.id}")
//        }

        web_view.loadDataWithBaseURL("http://www.ftchinese.com", html, "text/html", null, null)

        saveHistory()

        if (BuildConfig.DEBUG) {
            val fileName = mChannelItem?.cacheHTMLName ?: return
            GlobalScope.launch {
                mFileCache?.saveText(fileName, html)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        mLoadJob?.cancel()
        mRefreshJob?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        mLoadJob?.cancel()
        mRefreshJob?.cancel()
    }

    override fun onRefresh() {
        toast(R.string.prompt_refreshing)

        if (!isNetworkConnected()) {
            toast(R.string.prompt_no_network)
            return
        }

        // If user tries to refresh while content is still loading, stop it.
        if (mLoadJob?.isActive == true) {
            mLoadJob?.cancel()
        }

        mRefreshJob = GlobalScope.launch(Dispatchers.Main) {
            if (mTemplate == null) {
                mTemplate = mFileCache?.readStoryTemplate()
            }

            loadFromServer()
        }
    }

    private fun saveHistory() {
        val item = mChannelItem ?: return
        GlobalScope.launch {

            info("Save reading history")
            mArticleStore?.addHistory(item)
        }
    }

    companion object {
        private const val EXTRA_CHANNEL_ITEM = "extra_channel_item"

        /**
         * Start this activity
         */
        fun start(context: Context?, channelItem: ChannelItem) {
            val intent = Intent(context, StoryActivity::class.java)
            intent.putExtra(EXTRA_CHANNEL_ITEM, json.toJsonString(channelItem))
            context?.startActivity(intent)
        }
    }
}
