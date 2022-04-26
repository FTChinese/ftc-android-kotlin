package com.ft.ftchinese.ui.webpage

import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import com.ft.ftchinese.model.content.*
import com.ft.ftchinese.model.enums.ArticleType
import com.ft.ftchinese.model.fetch.marshaller
import com.ft.ftchinese.tracking.Sponsor
import com.ft.ftchinese.tracking.SponsorManager
import com.ft.ftchinese.ui.article.ArticleActivity
import com.ft.ftchinese.ui.channel.ChannelActivity
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.serialization.decodeFromString
import org.jetbrains.anko.toast

private const val TAG = "JsInterface"

interface JsEventListener {
    fun onClosePage()
    fun onProgress(loading: Boolean)
    fun onAlert(message: String)
    fun onTeaserClicked(teaser: Teaser)
    fun onChannelClicked(source: ChannelSource)
    fun onFollowTopic(following: Following)
}

open class BaseJsEventListener(
    private val context: Context,
    private val channelSource: ChannelSource? = null
) : JsEventListener {
    private val topicStore = FollowingManager.getInstance(context)

    override fun onClosePage() {

    }

    override fun onProgress(loading: Boolean) {

    }

    override fun onAlert(message: String) {
        context.toast(message)
    }

    override fun onTeaserClicked(teaser: Teaser) {
        ArticleActivity.start(
            context,
            teaser.withParentPerm(channelSource?.permission)
        )
    }

    override fun onChannelClicked(source: ChannelSource) {
        ChannelActivity.start(context, source)
    }

    override fun onFollowTopic(following: Following) {
        val isSubscribed = topicStore.save(following)

        if (isSubscribed) {
            FirebaseMessaging.getInstance()
                .subscribeToTopic(following.topic)
                .addOnCompleteListener { task ->
                    Log.i(ArticleActivity.TAG, "Subscribing to topic ${following.topic} success: ${task.isSuccessful}")
                }
        } else {
            FirebaseMessaging.getInstance()
                .unsubscribeFromTopic(following.topic)
                .addOnCompleteListener { task ->
                    Log.i(ArticleActivity.TAG, "Unsubscribing from topic ${following.topic} success: ${task.isSuccessful}")
                }
        }
    }
}

class JsInterface(
    private val listener: JsEventListener
) {

    private var teasers: List<Teaser> = listOf()

    @JavascriptInterface
    fun wvClosePage() {
        listener.onClosePage()
    }

    @JavascriptInterface
    fun wvProgress(loading: Boolean = false) {
        listener.onProgress(loading)
    }

    @JavascriptInterface
    fun wvAlert(msg: String) {
        listener.onAlert(msg)
    }

    /**
     * Used when a channel page loaded.
     * After HTML is loaded into webview, it will call this
     * method in JS and a list of Teaser is posted.
     */
    @JavascriptInterface
    fun onPageLoaded(message: String) {

        Log.i(TAG, "JS onPageLoaded")

        val channelContent = marshaller.decodeFromString<ChannelContent>(message)

        // Save all teasers.
        val articleList = channelContent.sections[0].lists[0].items
        Log.i(TAG, "Channel teasers $articleList")

        val channelMeta = channelContent.meta

        teasers = articleList.map {
            it.withMeta(channelMeta)
        }
    }

    /**
     * Used when an item is clicked on a channel page.
     * The clicked item might be an article, or another channel page.
     */
    @JavascriptInterface
    fun onSelectItem(index: String) {
        Log.i(TAG, "JS select item: $index")

        val i = try {
            index.toInt()
        } catch (e: Exception) {
            -1
        }

        articleListItemSelected(i)
    }

    private fun articleListItemSelected(index: Int) {
        Log.i(TAG, "JS interface responding to click on an item")

        // Find which item user is clicked.
        teasers
            .getOrNull(index)
            ?.let {
                /**
                 * {
                 * "id": "007000049",
                 * "type": "column",
                 * "headline": "徐瑾经济人" }
                 * Canonical URL: http://www.ftchinese.com/channel/column.html
                 * Content URL: https://api003.ftmailbox.com/column/007000049?webview=ftcapp&bodyonly=yes
                 */
                if (it.type == ArticleType.Column) {
                    listener.onChannelClicked(
                        ChannelSource.fromTeaser(it)
                    )
                } else {
                    /**
                     * For this type of data, load url directly.
                     * Teaser(
                     * id=44330,
                     * type=interactive,
                     * subType=mbagym,
                     * title=一周新闻小测：2021年07月17日,
                     * audioUrl=null,
                     * radioUrl=null,
                     * publishedAt=null,
                     * tag=FT商学院,教程,一周新闻,入门级,FTQuiz,AITranslation)
                     */
                    listener.onTeaserClicked(it)
                }
            }
    }

    @JavascriptInterface
    fun onLoadedSponsors(message: String) {
        Log.i(TAG, "Loaded sponsors: $message")

        marshaller.decodeFromString<List<Sponsor>>(message).let {
            SponsorManager.sponsors = it
        }
    }

    /**
     * Used to handle follow topics in an article page.
     */
    @JavascriptInterface
    fun follow(message: String) {
        Log.i(TAG, "Clicked follow: $message")

        listener.onFollowTopic(marshaller.decodeFromString(message))
    }
}
