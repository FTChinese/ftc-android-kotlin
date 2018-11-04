package com.ft.ftchinese

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import com.ft.ftchinese.models.ChannelItem
import com.ft.ftchinese.models.FollowingManager
import com.ft.ftchinese.util.gson
import kotlinx.android.synthetic.main.activity_content.*
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

/**
 * WebContentActivity might be launched in two circumstances:
 * 1. Clicked on a link directly;
 * 2. A click event is intercepted by JS and JS executed Java method injected into the WebView and passed some data back (mostly ChannelItem tier)
 *
 * Activities start this must pass the web page's parsed canonical uri (the url that could be directly opened in a browser), which will be used for social share.
 */
class WebContentActivity : AbsContentActivity() {

    override val articleWebUrl: String
        get() = mChannelItem?.canonicalUrl ?: ""

    override val articleTitle: String
        get() = ""

    override val articleStandfirst: String
        get() = ""

    override var mChannelItem: ChannelItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val itemDate = intent.getStringExtra(EXTRA_CHANNEL_ITEM)

        if (itemDate != null) {
            try {
                mChannelItem = gson.fromJson(itemDate, ChannelItem::class.java)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val url = mChannelItem?.apiUrl ?: return

        info("Start loading a url directly into webview: $url")
        load(url)
        updateStarUI()
    }

    override fun onDestroy() {
        super.onDestroy()

        info("Activity destroyed")
    }

    override fun onRefresh() {
        toast(R.string.prompt_refreshing)

        web_view.reload()
        showProgress(false)
    }

    override fun load() {

    }

    fun load(url: String) {
        web_view.loadUrl(url)
        showProgress(false)
    }

    companion object {
        private const val EXTRA_CANONICAL_URI = "extra_canonical_uri"
        private const val EXTRA_CHANNEL_ITEM = "extra_channel_item"

        fun start(context: Context?, url: Uri) {
            val intent = Intent(context, WebContentActivity::class.java)
            intent.putExtra(EXTRA_CANONICAL_URI, url)
            context?.startActivity(intent)
        }

        fun start(context: Context?, channelItem: ChannelItem) {
            val intent = Intent(context, WebContentActivity::class.java)
            intent.putExtra(EXTRA_CHANNEL_ITEM, gson.toJson(channelItem))
            context?.startActivity(intent)
        }
    }
}
