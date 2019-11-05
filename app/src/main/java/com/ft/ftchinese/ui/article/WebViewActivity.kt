package com.ft.ftchinese.ui.article

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.KeyEvent
import com.ft.ftchinese.R
import kotlinx.android.synthetic.main.activity_web_view.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

private const val EXTRA_WEB_URL = "extra_web_url"

class WebViewActivity : AppCompatActivity(), AnkoLogger {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)

        setSupportActionBar(web_toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }

        val url = intent.getStringExtra(EXTRA_WEB_URL) ?: return

        info("Loading url: $url")

        web_content.loadUrl(url)

        web_content.settings.apply {
            javaScriptEnabled = true
            loadsImagesAutomatically = true
            domStorageEnabled = true
            databaseEnabled = true
        }

        web_content.apply {

            setOnKeyListener { _, keyCode, _ ->
                if (keyCode == KeyEvent.KEYCODE_BACK && web_content.canGoBack()) {
                    web_content.goBack()
                    return@setOnKeyListener true
                }
                false
            }
        }
    }

    companion object {
        @JvmStatic fun start(context: Context?, url: String) {
            context?.startActivity(Intent(context, WebViewActivity::class.java).apply {
                putExtra(EXTRA_WEB_URL, url)
            })
        }
    }
}
