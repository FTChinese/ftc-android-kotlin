package com.ft.ftchinese.ui.web

import android.annotation.SuppressLint
import android.view.KeyEvent
import android.webkit.WebView
import com.ft.ftchinese.ui.base.JS_INTERFACE_NAME

@SuppressLint("SetJavaScriptEnabled")
fun configWebView(webView: WebView, jsInterface: JsInterface, client: WVClient) {
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
                JS_INTERFACE_NAME
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
