package com.ft.ftchinese

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

class WebContentActivity : AbstractContentActivity() {

    private var urlToLoad: String? = null

    companion object {
        private const val EXTRA_WEB_URL = "extra_web_url"

        fun start(context: Context?, url: String) {
            val intent = Intent(context, WebContentActivity::class.java)
            intent.putExtra(EXTRA_WEB_URL, url)
            context?.startActivity(intent)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        urlToLoad = intent.getStringExtra(EXTRA_WEB_URL)

        init()
    }

    override fun onRefresh() {
        super.onRefresh()

        init()
    }

    override fun init() {
        if (urlToLoad != null) {
            loadUrl(urlToLoad!!)
        } else {
            Toast.makeText(this, "Failed to load data.", Toast.LENGTH_SHORT).show()
        }
    }
}
