package com.ft.ftchinese.ui.web

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebView
import com.ft.ftchinese.store.WebViewAccessTokenCookieManager

fun routeWebViewBridgeLink(
    webView: WebView?,
    callback: WebViewCallback,
    url: String,
): Boolean {
    if (url.isBlank()) {
        Log.i(WEB_PURCHASE_FLOW_TAG, "bridge_link ignored_blank_url")
        return false
    }

    Log.i(WEB_PURCHASE_FLOW_TAG, "bridge_link url=${debugWebUrl(url)}")
    Handler(Looper.getMainLooper()).post {
        WebViewAccessTokenCookieManager.syncAccessTokenForUrl(webView, url)
        callback.onOverrideUrlLoading(url)
    }
    return true
}
