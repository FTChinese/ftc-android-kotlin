package com.ft.ftchinese.ui.components

import android.util.Log
import android.webkit.JavascriptInterface
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ft.ftchinese.model.content.ChannelContent
import com.ft.ftchinese.model.content.Following
import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.model.fetch.marshaller
import com.ft.ftchinese.tracking.Sponsor
import com.ft.ftchinese.tracking.SponsorManager
import kotlinx.serialization.decodeFromString

private const val TAG = "WebInterfaceViewModel"

class WebClientViewModel : ViewModel() {

    val progressLiveData = MutableLiveData<Boolean>()
    val exitLiveData = MutableLiveData(false)
    val alertLiveData = MutableLiveData("")

    val teaserSelected: MutableLiveData<Teaser> by lazy {
        MutableLiveData<Teaser>()
    }

    val sponsorsReceived: MutableLiveData<List<Sponsor>> by lazy {
        MutableLiveData<List<Sponsor>>()
    }

    val followTagClicked: MutableLiveData<Following> by lazy {
        MutableLiveData<Following>()
    }

    private var teasers: List<Teaser> = listOf()

    @JavascriptInterface
    fun wvClosePage() {
        exitLiveData.postValue(true)
    }

    @JavascriptInterface
    fun wvProgress(loading: Boolean = false) {
        progressLiveData.postValue(loading)
    }

    @JavascriptInterface
    fun wvAlert(msg: String) {
        alertLiveData.postValue(msg)
    }

    /**
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
        if (index < 0 || index >= teasers.size) {
            return
        }

        teasers[index].let {
            teaserSelected.postValue(it)
        }
    }

    @JavascriptInterface
    fun onLoadedSponsors(message: String) {

        Log.i(TAG, "Loaded sponsors: $message")

        marshaller.decodeFromString<List<Sponsor>>(message).let {
            sponsorsReceived.postValue(it)
            SponsorManager.sponsors = it
        }
    }

    @JavascriptInterface
    fun follow(message: String) {
        Log.i(TAG, "Clicked follow: $message")

        marshaller.decodeFromString<Following>(message)
            .let {
                followTagClicked.postValue(it)
            }
    }
}
