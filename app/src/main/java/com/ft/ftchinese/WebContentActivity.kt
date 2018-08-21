package com.ft.ftchinese

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import org.jetbrains.anko.info

class WebContentActivity : AbsContentActivity() {

    private var urlToLoad: String? = null

    companion object {
        private const val EXTRA_WEB_URL = "extra_web_url"

        fun start(context: Context?, url: String?) {
            if (url == null) {
                Toast.makeText(context, "Cannot load data", Toast.LENGTH_SHORT).show()
                return
            }
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

    override fun onDestroy() {
        super.onDestroy()

        info("Activity destroyed")
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
