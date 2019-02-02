package com.ft.ftchinese

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import com.ft.ftchinese.models.ChannelItem
import com.ft.ftchinese.models.json
import kotlinx.android.synthetic.main.activity_content.*
import org.jetbrains.anko.info

class RadioActivity : AbsContentActivity() {

    override var mChannelItem: ChannelItem? = null

    override val articleWebUrl: String
        get() = mChannelItem?.canonicalUrl ?: ""

    override val articleTitle: String
        get() = mChannelItem?.headline ?: ""

    override val articleStandfirst: String
        get() = mChannelItem?.standfirst ?: mChannelItem?.headline ?: ""

    companion object {
        private const val EXTRA_CHANNEL_ITEM = "extra_channel_item"

        fun start(context: Context?, channelItem: ChannelItem) {
            val intent = Intent(context, RadioActivity::class.java)
            intent.putExtra(EXTRA_CHANNEL_ITEM, json.toJsonString(channelItem))
            context?.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_radio)

//        supportActionBar?.apply {
//            setDisplayHomeAsUpEnabled(true)
//            setDisplayShowTitleEnabled(false)
//        }

        swipe_refresh.isEnabled = false
        language_group.visibility = View.GONE

        web_view.settings.apply {
            javaScriptEnabled = true
            loadsImagesAutomatically = true
        }

        web_view.apply {

            webViewClient = WVClient(this@RadioActivity)

            setOnKeyListener { v, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && web_view.canGoBack()) {
                    web_view.goBack()
                    return@setOnKeyListener true
                }

                false
            }
        }

        val itemData = intent.getStringExtra(EXTRA_CHANNEL_ITEM)

        mChannelItem = json.parse<ChannelItem>(itemData) ?: return
        info("Creating radio activity for $mChannelItem, API URL: ${mChannelItem?.apiUrl}")

        updateStarUI()

        loadUrl(mChannelItem?.apiUrl)
    }

    /**
     * Stub implementation
     */
    override fun onRefresh() {
        web_view.reload()
    }

    private fun loadUrl(url: String?) {
        web_view.loadUrl(url)

        logViewItemEvent()
    }
}
