package com.ft.ftchinese

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.ft.ftchinese.models.*
import com.ft.ftchinese.user.SignInActivity
import com.ft.ftchinese.user.SubscriptionActivity
import com.ft.ftchinese.util.Store
import com.ft.ftchinese.util.gson
import com.ft.ftchinese.util.isNetworkConnected
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Request
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.JsonSyntaxException
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
        get() = mChannelItem?.headline ?: ""

    override val articleStandfirst: String
        get() = mChannelItem?.standfirst ?: ""

    private var mCurrentLanguage: Int = ChannelItem.LANGUAGE_CN

    // Hold metadata on where and how to find data for this page.
    override var mChannelItem: ChannelItem? = null

    private var mLoadJob: Job? = null
    private var mCacheJob: Job? = null
    private var mRefreshJob: Job? = null

    private var mRequest: Request? = null

    private var mTemplate: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        // Meta data about current article
        val itemData = intent.getStringExtra(EXTRA_CHANNEL_ITEM)

        if (itemData != null) {
            try {
                mChannelItem = gson.fromJson(itemData, ChannelItem::class.java)


            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        titlebar_cn.setOnClickListener {
            mCurrentLanguage = ChannelItem.LANGUAGE_CN
            loadContent()
        }

        titlebar_en.setOnClickListener {
            val user = mSessionManager?.loadUser()

            if (user == null) {
                enableLanguageSwitch = false

                toast(R.string.prompt_restricted_login)

                SignInActivity.start(this)
                return@setOnClickListener
            }

            if (!user.canAccessPaidContent) {
                enableLanguageSwitch = false

                toast(R.string.prompt_restricted_paid_user)
                SubscriptionActivity.start(this)
                return@setOnClickListener
            }

            mCurrentLanguage = ChannelItem.LANGUAGE_EN
            loadContent()
        }

        titlebar_bi.setOnClickListener {
            val user = mSessionManager?.loadUser()

            if (user == null) {
                enableLanguageSwitch = false

                toast(R.string.prompt_restricted_login)

                SignInActivity.start(this)
                return@setOnClickListener
            }

            if (!user.canAccessPaidContent) {
                enableLanguageSwitch = false

                toast(R.string.prompt_restricted_paid_user)

                SubscriptionActivity.start(this)
                return@setOnClickListener
            }

            mCurrentLanguage = ChannelItem.LANGUAGE_BI
            loadContent()
        }


        info("Start loading article: $mChannelItem")
        loadContent()
        updateStarUI()
    }

    private fun loadContent() {

        mLoadJob = GlobalScope.launch(Dispatchers.Main) {

            try {

                val ok = loadFromCache()

                if (!ok) {
                    loadFromServer()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                toast("${e.message}")
            }
        }

        mFirebaseAnalytics?.logEvent(FirebaseAnalytics.Event.VIEW_ITEM, Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, mChannelItem?.id)
            putString(FirebaseAnalytics.Param.ITEM_NAME, mChannelItem?.headline)
            putString(FirebaseAnalytics.Param.ITEM_CATEGORY, mChannelItem?.type)
        })
    }

    private suspend fun loadFromCache(): Boolean {
        if (mTemplate == null) {
            mTemplate = Store.readStoryTemplate(resources)
        }

        val jsonData = Store.load(this, mChannelItem?.cacheFileName) ?: return false

        renderAndLoad(jsonData)

        return true
    }

    private suspend fun loadFromServer() {

        if (!isNetworkConnected()) {
            toast(R.string.prompt_no_network)
            return
        }

        val url = mChannelItem?.apiUrl ?: return

        if (mTemplate == null) {
            mTemplate = Store.readStoryTemplate(resources)
        }

        if (!swipe_refresh.isRefreshing) {
            isInProgress = true
        }

        mRequest = Fuel.get(url)
                .responseString { _, _, result ->
                    isInProgress = false

                    val (data, error) = result

                    if (error != null || data == null) {
                        toast(R.string.prompt_load_failure)
                        return@responseString
                    }

                    renderAndLoad(data)

                    cacheData(data)
                }
    }

    private fun cacheData(data: String) {
        mCacheJob = Store.save(this@StoryActivity, mChannelItem?.cacheFileName, data)
    }

    private fun renderAndLoad(data: String) {

        val story = try {
            gson.fromJson<Story>(data, Story::class.java)
        } catch (e: JsonSyntaxException) {
            toast(R.string.prompt_load_failure)
            return
        }

        showLanguageSwitch = story.isBilingual

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
                mTemplate = Store.readStoryTemplate(resources)
            }

            loadFromServer()
        }
    }

//    private fun updateFavouriteIcon() {
//        action_favourite.setImageResource(if (mIsStarring) R.drawable.ic_favorite_teal_24dp else R.drawable.ic_favorite_border_teal_24dp )
//    }

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
            intent.putExtra(EXTRA_CHANNEL_ITEM, gson.toJson(channelItem))
            context?.startActivity(intent)
        }
    }
}
