package com.ft.ftchinese.ui.web

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.accompanist.web.WebView
import com.google.accompanist.web.WebViewState

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ComposeWebView(
    wvState: WebViewState,
    modifier: Modifier = Modifier,
    webClientCallback: WebViewCallback = rememberWebViewCallback(),
    jsListener: JsEventListener = rememberJsEventListener(),
    onCreated: (WebView) -> Unit = {}
) {

    val chromeClient = remember {
        ComposeChromeClient()
    }

    val jsInterface = remember(jsListener) {
        JsInterface(jsListener)
    }

    val webClient = remember(webClientCallback) {
        ComposeWebViewClient(
            callback = webClientCallback
        )
    }

    WebView(
        state = wvState,
        modifier = modifier
            .fillMaxWidth(),
        captureBackPresses = false,
        onCreated = { webView ->
            webView.settings.javaScriptEnabled = true
            webView.settings.loadsImagesAutomatically = true
            webView.settings.domStorageEnabled = true
            webView.settings.databaseEnabled = true
            webView.addJavascriptInterface(jsInterface, JS_INTERFACE_NAME)
            onCreated(webView)
        },
        client = webClient,
        chromeClient = chromeClient,
    )
}
