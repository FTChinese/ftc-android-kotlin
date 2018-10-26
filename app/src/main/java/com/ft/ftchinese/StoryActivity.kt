package com.ft.ftchinese

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.ft.ftchinese.database.ArticleStore
import com.ft.ftchinese.models.Story
import com.ft.ftchinese.models.ChannelItem
import com.ft.ftchinese.util.gson
import com.ft.ftchinese.util.isNetworkConnected
import kotlinx.android.synthetic.main.activity_content.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.coroutines.experimental.bg
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
    private var mChannelItem: ChannelItem? = null

    private var job: Job? = null

    private var mTemplate: String? = null
    private var mStory: Story? = null

    private var mIsStarring: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Meta data about current article
        val itemData = intent.getStringExtra(EXTRA_CHANNEL_ITEM)

        if (itemData != null) {
            mChannelItem = gson.fromJson(itemData, ChannelItem::class.java)
        }


        action_favourite.setOnClickListener {
            mIsStarring = !mIsStarring

            updateFavouriteIcon()

            if (mIsStarring) {
                ArticleStore.getInstance(this).addStarred(mChannelItem)
            } else {
                ArticleStore.getInstance(this).deleteStarred(mChannelItem)
            }
        }

        titlebar_cn.setOnClickListener {
            mCurrentLanguage = ChannelItem.LANGUAGE_CN
            load()
        }

        titlebar_en.setOnClickListener {
            mCurrentLanguage = ChannelItem.LANGUAGE_EN
            load()
        }

        titlebar_bi.setOnClickListener {
            mCurrentLanguage = ChannelItem.LANGUAGE_BI
            load()
        }

        load()

        info("onCreate finished")
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
    }

    override fun onRefresh() {
        toast(R.string.prompt_refreshing)

        job = launch(UI) {
            if (mTemplate == null) {
                mTemplate = ChannelItem.readTemplateAsync(resources).await()
            }
            fetchAndUpdate()
        }
    }

    override fun load() {

        job = launch(UI) {
            mTemplate = ChannelItem.readTemplateAsync(resources).await()

            if (mTemplate == null) {
                toast(R.string.prompt_load_failure)
                return@launch
            }

            mStory = mChannelItem?.loadCachedStoryAsync(this@StoryActivity)?.await()

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
        }

        mIsStarring = ArticleStore.getInstance(this).isStarring(mChannelItem)
        updateFavouriteIcon()
    }

    private suspend fun fetchAndUpdate() {
        mStory = mChannelItem?.fetchStoryAsync(this)?.await()

        loadData()
    }

    private fun loadData() {
        showLanguageSwitch()

        val html = mChannelItem?.render(this@StoryActivity, mCurrentLanguage, mTemplate, mStory)

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

    private fun updateFavouriteIcon() {
        action_favourite.setImageResource(if (mIsStarring) R.drawable.ic_favorite_teal_24dp else R.drawable.ic_favorite_border_teal_24dp )
    }

    private fun saveHistory() {
        val item = mChannelItem ?: return
        bg {

            info("Save reading history")
            ArticleStore.getInstance(context = this@StoryActivity).addHistory(item)
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
