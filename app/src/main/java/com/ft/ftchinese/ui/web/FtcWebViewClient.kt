package com.ft.ftchinese.ui.web

import android.graphics.Bitmap
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.google.accompanist.web.AccompanistWebViewClient

private const val TAG = "WebClient"

class FtcWebViewClient(
    private val callback: WebViewCallback,
) : AccompanistWebViewClient() {

    var pageStarted: Boolean = false
        private set

    var pageFinished: Boolean = false
        private set

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        pageStarted = true
        callback.onPageStarted(view, url)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        pageFinished = true
        callback.onPageFinished(view, url)
    }

    /**
     * Override accompanist's implementation as it has a very weired behavior.
     */
    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {


        val uri = request?.url ?: return super.shouldOverrideUrlLoading(view, request)

        Log.i(TAG, "shouldOverrideUrlLoading: $uri")

        callback.onOverrideUrlLoading(WvUrlEvent.fromUri(uri))
        return true
    }
}

@Composable
fun rememberFtcWebViewClient(
    callback: WebViewCallback = rememberWebViewCallback()
) = remember(callback) {
    FtcWebViewClient(
        callback
    )
}
