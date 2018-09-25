package com.ft.ftchinese

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
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
        get() = canonicalUri.toString()

    override val articleTitle: String
        get() = ""

    override val articleStandfirst: String
        get() = ""

    private var canonicalUri: Uri? = null
    private var apiUrl: String? = null

    private fun buildUrl(uri: Uri?, path: String? = null): String? {
        if (uri == null) {
            return null
        }
        val builder = uri.buildUpon()
                .scheme("https")
                .authority("api003.ftmailbox.com")
                .appendQueryParameter("bodyonly", "yes")
                .appendQueryParameter("webview", "ftcapp")

        if (path != null) {
            builder.path(path)
        }

        return builder.build().toString()
    }

    companion object {
        private const val EXTRA_CANONICAL_URI = "extra_canonical_uri"

        fun start(context: Context?, url: Uri) {
            val intent = Intent(context, WebContentActivity::class.java)
            intent.putExtra(EXTRA_CANONICAL_URI, url)
            context?.startActivity(intent)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        canonicalUri = intent.getParcelableExtra(EXTRA_CANONICAL_URI)

        if (canonicalUri != null) {
            apiUrl = buildUrl(canonicalUri, null)
        }

        action_favourite.visibility = View.GONE

        load()
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
        if (apiUrl != null) {
            web_view.loadUrl(apiUrl)
            showProgress(false)
        } else {
            toast(R.string.prompt_load_failure)
        }
    }

}
