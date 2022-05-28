package com.ft.ftchinese.ui.web

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

class DumbWebViewClient : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        return true
    }
}
