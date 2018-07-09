package com.ft.ftchinese

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View
import android.webkit.WebView
import android.widget.Toast

import kotlinx.android.synthetic.main.activity_content.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch

class ContentActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_content)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }

        swipe_refresh.setOnRefreshListener(this)


        init()

    }

    private fun init() {
        if (!swipe_refresh.isRefreshing) {
            progress_bar.visibility = View.VISIBLE
        }
        launch(UI) {
            val templateFile = async { readHtml(resources, R.raw.story) }

            val html = templateFile.await()

            if (html != null) {
                webview.loadDataWithBaseURL("http://www.ftchinese.com", html, "text/html", null, null)
            }

            swipe_refresh.isRefreshing = false
            progress_bar.visibility = View.GONE
        }
    }

    override fun onRefresh() {
        Toast.makeText(this, "Refreshing", Toast.LENGTH_SHORT).show()
        init()
    }
}
