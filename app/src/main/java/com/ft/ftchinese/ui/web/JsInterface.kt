package com.ft.ftchinese.ui.web

import android.util.Log
import android.webkit.JavascriptInterface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.ft.ftchinese.model.content.ChannelContent
import com.ft.ftchinese.model.content.ChannelSource
import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.model.enums.ArticleType
import com.ft.ftchinese.model.fetch.marshaller
import com.ft.ftchinese.tracking.Sponsor
import com.ft.ftchinese.tracking.SponsorManager
import kotlinx.serialization.decodeFromString

private const val TAG = "JsInterface"

class JsInterface(
    private val listener: JsEventListener = DumbJsEventListener()
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

@Composable
fun rememberJsInterface(
    callback: JsEventListener = rememberJsEventListener()
) = remember(callback) {
    JsInterface(callback)
}
