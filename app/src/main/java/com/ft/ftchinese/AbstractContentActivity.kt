package com.ft.ftchinese

import android.content.Context
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import android.widget.Toast
import com.ft.ftchinese.models.Following
import com.ft.ftchinese.util.gson
import kotlinx.android.synthetic.main.activity_content.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

abstract class AbstractContentActivity : AppCompatActivity(),
        SwipeRefreshLayout.OnRefreshListener,
        AnkoLogger {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_content)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            // Do not show title on the toolbar for any content.
            setDisplayShowTitleEnabled(false)
        }

        swipe_refresh.setOnRefreshListener(this)

        web_view.settings.apply {
            javaScriptEnabled = true
            loadsImagesAutomatically = true
        }

        web_view.apply {

            addJavascriptInterface(WebAppInterface(), "Android")

            webViewClient = BaseWebViewClient(this@AbstractContentActivity)

            webChromeClient = MyChromeClient()

            setOnKeyListener { v, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && web_view.canGoBack()) {
                    web_view.goBack()
                    return@setOnKeyListener true
                }

                false
            }
        }

    }

    override fun onRefresh() {
        Toast.makeText(this, "Refreshing", Toast.LENGTH_SHORT).show()
    }

    abstract fun init()

    fun loadData(data: String?) {

        info("Load HTML string into web view")

        web_view.loadDataWithBaseURL("http://www.ftchinese.com", data, "text/html", null, null)

        stopProgress()
    }

    fun loadUrl(url: String) {

        info("Load url directly: $url")

        web_view.loadUrl(url)
        stopProgress()
    }

    fun showProgress() {
        progress_bar.visibility = View.VISIBLE
    }

    fun stopProgress() {
        swipe_refresh.isRefreshing = false
        progress_bar.visibility = View.GONE
    }

    inner class WebAppInterface : AnkoLogger {

        /**
         * Usage in JS: Android.follow(message)
         */
        @JavascriptInterface
        fun follow(message: String) {
            info("Clicked a follow button")
            info("Received follow message: $message")

            val following = gson.fromJson<Following>(message, Following::class.java)
            following.save(this@AbstractContentActivity)
        }
    }
}