package com.ft.ftchinese.ui.web

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.ft.ftchinese.ui.base.JS_INTERFACE_NAME
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.WebView
import com.google.accompanist.web.WebViewState

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SimpleWebView(
    state: WebViewState,
    jsInterface: JsInterface,
) {

    val webClient = remember {
        object : AccompanistWebViewClient() {
            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
            }
        }
    }

    WebView(
        state = state,
        modifier = Modifier.fillMaxSize(),
        captureBackPresses = false,
        onCreated = { webView ->
            webView.settings.javaScriptEnabled = true
            webView.settings.loadsImagesAutomatically = true
            webView.settings.domStorageEnabled = true
            webView.settings.databaseEnabled = true
            webView.addJavascriptInterface(jsInterface, JS_INTERFACE_NAME)
        },
        client = webClient,
    )
}



