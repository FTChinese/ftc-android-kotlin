package com.ft.ftchinese

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import com.ft.ftchinese.models.ChannelItem
import com.ft.ftchinese.utils.gson

import kotlinx.android.synthetic.main.activity_radio.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class RadioActivity : AppCompatActivity(), AnkoLogger {

    private var channelItem: ChannelItem? = null

    companion object {
        private const val EXTRA_CHANNEL_ITEM = "extra_channel_item"

        fun start(context: Context?, channelItem: ChannelItem) {
            val intent = Intent(context, RadioActivity::class.java)
            intent.putExtra(EXTRA_CHANNEL_ITEM, gson.toJson(channelItem))
            context?.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_radio)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }

        web_view.settings.apply {
            javaScriptEnabled = true
            loadsImagesAutomatically = true
        }

        web_view.apply {

            webViewClient = BaseWebViewClient(this@RadioActivity)

            setOnKeyListener { v, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && web_view.canGoBack()) {
                    web_view.goBack()
                    return@setOnKeyListener true
                }

                false
            }
        }

        val itemData = intent.getStringExtra(EXTRA_CHANNEL_ITEM)

        channelItem = gson.fromJson(itemData, ChannelItem::class.java)
        info("Creating radio activity for $channelItem")

        val audioUrl = channelItem?.shortlead

        loadUrl(channelItem?.apiUrl)
    }

    private fun loadUrl(url: String?) {

        info("Load url directly: $url")

        web_view.loadUrl(url)
    }
}
