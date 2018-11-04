package com.ft.ftchinese

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.ft.ftchinese.models.*
import com.ft.ftchinese.user.SignInActivity
import com.ft.ftchinese.user.SubscriptionActivity
import com.ft.ftchinese.util.Store
import com.ft.ftchinese.util.gson
import com.ft.ftchinese.util.isNetworkConnected
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
    private var mRefreshJob: Job? = null

    private var mTemplate: String? = null
    private var mStory: Story? = null

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
            load()
        }

        titlebar_en.setOnClickListener {
            val user = mSessionManager?.loadUser()

            if (user == null) {
                languageSwitchFobidden()

                toast(R.string.prompt_restricted_login)

                SignInActivity.start(this)
                return@setOnClickListener
            }

            if (!user.canAccessPaidContent) {
                languageSwitchFobidden()

                toast(R.string.prompt_restricted_paid_user)
                SubscriptionActivity.start(this)
                return@setOnClickListener
            }

            mCurrentLanguage = ChannelItem.LANGUAGE_EN
            load()
        }

        titlebar_bi.setOnClickListener {
            val user = mSessionManager?.loadUser()

            if (user == null) {
                languageSwitchFobidden()
                toast(R.string.prompt_restricted_login)

                SignInActivity.start(this)
                return@setOnClickListener
            }

            if (!user.canAccessPaidContent) {
                languageSwitchFobidden()
                toast(R.string.prompt_restricted_paid_user)

                SubscriptionActivity.start(this)
                return@setOnClickListener
            }

            mCurrentLanguage = ChannelItem.LANGUAGE_BI
            load()
        }


        info("Start loading article: $mChannelItem")
        load()
        updateStarUI()
    }

    private fun languageSwitchFobidden() {
        titlebar_cn.isChecked = true
        titlebar_en.isChecked = false
        titlebar_bi.isChecked = false
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
                mTemplate = async {
                    Store.readStoryTemplate(resources)
                }.await()
            }
            fetchAndUpdate()
        }
    }

    override fun load() {

        mLoadJob = GlobalScope.launch(Dispatchers.Main) {
            mTemplate = async {
                Store.readStoryTemplate(resources)
            }.await()

            if (mTemplate == null) {
                toast(R.string.prompt_load_failure)
                return@launch
            }

            try {
                mStory = async {
                    mChannelItem?.loadCachedStory(this@StoryActivity)
                }.await()

                // Use cached data to render mTemplate
                if (mStory != null) {
                    loadData()

                    return@launch
                }

                if (!isNetworkConnected()) {
                    toast(R.string.prompt_no_network)
                    return@launch
                }

                // If it reached here, it indicates no cache found, and network is connected.
                showProgress(true)
                fetchAndUpdate()
            } catch (e: Exception) {
                e.printStackTrace()
                toast("${e.message}")
            }
        }
    }

    private suspend fun fetchAndUpdate() {
        mStory = GlobalScope.async {
            mChannelItem?.fetchStory(this@StoryActivity)
        }.await()

        loadData()
    }

    private fun loadData() {
        showLanguageSwitch()
        val follows = mFollowingManager?.loadForJS() ?: JSFollows(mapOf())

        val html = mChannelItem?.render(mTemplate, mStory, mCurrentLanguage, follows = follows)

        if (html == null) {
            showProgress(false)
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

        showProgress(false)

        saveHistory()
    }

    private fun showLanguageSwitch() {
        language_group.visibility = if (mStory?.isBilingual == true) View.VISIBLE else View.GONE
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
