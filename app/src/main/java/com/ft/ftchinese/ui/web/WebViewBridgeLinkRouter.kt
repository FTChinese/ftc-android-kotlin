package com.ft.ftchinese.ui.web

import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import com.ft.ftchinese.store.WebViewAccessTokenCookieManager

fun routeWebViewBridgeLink(
    webView: WebView?,
    callback: WebViewCallback,
    url: String,
): Boolean {
    if (url.isBlank()) {
        return false
    }

    Handler(Looper.getMainLooper()).post {
        WebViewAccessTokenCookieManager.syncAccessTokenForUrl(webView, url)
        callback.onOverrideUrlLoading(url)
    }
    return true
}
