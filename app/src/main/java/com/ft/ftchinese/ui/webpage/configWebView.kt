package com.ft.ftchinese.ui.webpage

import android.annotation.SuppressLint
import android.view.KeyEvent
import android.webkit.WebView
import android.webkit.WebViewClient
import com.ft.ftchinese.ui.web.ChromeClient
import com.ft.ftchinese.ui.web.JsInterface

@Deprecated("")
@SuppressLint("SetJavaScriptEnabled")
fun configWebView(webView: WebView, jsInterface: JsInterface, client: WebViewClient) {
    webView.settings.apply {
        javaScriptEnabled = true
        loadsImagesAutomatically = true
        domStorageEnabled = true
        databaseEnabled = true
    }

    webView.apply {
        apply {
            addJavascriptInterface(
                jsInterface,
                "Android"
            )

            webViewClient = client
            webChromeClient = ChromeClient()

            setOnKeyListener { _, keyCode, _ ->
                if (keyCode == KeyEvent.KEYCODE_BACK && canGoBack()) {
                    goBack()
                    return@setOnKeyListener true
                }
                false
            }
        }
    }
}
