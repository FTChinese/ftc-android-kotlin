package com.ft.ftchinese

import android.content.Context
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import android.widget.Toast
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

            val msg = gson.fromJson<FollowMessage>(message, FollowMessage::class.java)
            followOrUnfollow(msg)
        }

        private fun followOrUnfollow(msg: FollowMessage) {
            val sharedPrefs = getSharedPreferences("following", Context.MODE_PRIVATE)
            val hs = sharedPrefs.getStringSet(msg.type, HashSet<String>())
            info("Current shared prefernce: $hs")
            val newHs = HashSet(hs)

            when (msg.action) {
                "follow" -> {
                    newHs.add(msg.tag)
                }

                "unfollow" -> {
                    newHs.remove(msg.tag)
                }
            }

            info("New set: $newHs")

            val editor = sharedPrefs.edit()
//                    editor.putString(msg.type, msg.tag)
            editor.putStringSet(msg.type, newHs)
            val result = editor.commit()
            info("Commit result: $result")
        }
    }
}