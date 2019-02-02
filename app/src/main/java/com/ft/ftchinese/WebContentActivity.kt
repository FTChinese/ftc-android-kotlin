package com.ft.ftchinese

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import com.ft.ftchinese.models.ChannelItem
import com.ft.ftchinese.models.json
import com.ft.ftchinese.util.isNetworkConnected
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
        get() = mCanonicalUrl ?: ""

    override val articleTitle: String
        get() = ""

    override val articleStandfirst: String
        get() = ""

    override var mChannelItem: ChannelItem? = null

    private var mCanonicalUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent.getParcelableExtra<Uri>(EXTRA_UNKNOWN_URI)



        swipe_refresh.isEnabled = false

        if (uri != null) {
            mCanonicalUrl = uri.toString()

            bottom_menu_container.visibility = View.GONE
            load(buildUrl(uri))

            return
        }

        val itemData = intent.getStringExtra(EXTRA_CHANNEL_ITEM)

        info("Passed in data: $itemData")
        if (itemData != null) {

            try {
                mChannelItem = json.parse<ChannelItem>(itemData)
                mCanonicalUrl = mChannelItem?.canonicalUrl

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val url = mChannelItem?.apiUrl ?: return

        info("Start loading a url directly into webview: $url")
        load(url)
        updateStarUI()
    }

    private fun buildUrl(uri: Uri): String {
        val builder = uri.buildUpon()
        if (uri.getQueryParameter("webview") == null) {
            builder.appendQueryParameter("webview", "ftcapp")
        }

        return builder.build().toString()
    }

    override fun onDestroy() {
        super.onDestroy()

        info("Activity destroyed")
    }

    override fun onRefresh() {
    }

    fun load(url: String) {
        if (!isNetworkConnected()) {
            toast(R.string.prompt_no_network)

            return
        }

        web_view.loadUrl(url)

        logViewItemEvent()
    }

    companion object {
        private const val EXTRA_UNKNOWN_URI = "extra_unknown_uri"
        private const val EXTRA_CHANNEL_ITEM = "extra_channel_item"

        // Mainly used to handle unknown urls.
        // Since we do not know the exact structure of the uri,
        // we cannot turn it into a ChannelItem
        fun start(context: Context?, url: Uri) {
            val intent = Intent(context, WebContentActivity::class.java).apply {
                putExtra(EXTRA_UNKNOWN_URI, url)
            }

            context?.startActivity(intent)
        }

        // Mainly used to handle JSInterface click event and url clicks in web view clint.
        // The type and id must be known.
        fun start(context: Context?, channelItem: ChannelItem) {
            val intent = Intent(context, WebContentActivity::class.java).apply {
                putExtra(EXTRA_CHANNEL_ITEM, json.toJsonString(channelItem))
            }

            context?.startActivity(intent)
        }
    }
}
