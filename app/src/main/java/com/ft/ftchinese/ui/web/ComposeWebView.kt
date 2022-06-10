package com.ft.ftchinese.ui.web

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ft.ftchinese.ui.base.JS_INTERFACE_NAME
import com.ft.ftchinese.ui.theme.OColor
import com.google.accompanist.web.WebView
import com.google.accompanist.web.WebViewState

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ComposeWebView(
    wvState: WebViewState,
    webClient: ComposeWebViewClient,
    showProgress: Boolean = false,
    jsInterface: JsInterface = rememberJsInterface(),
    onCreated: (WebView) -> Unit = {}
) {

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        WebView(
            state = wvState,
            modifier = Modifier.fillMaxSize(),
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
        )

        if (showProgress && wvState.isLoading) {
            LinearProgressIndicator(
                color = OColor.claret,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
            )
        }
    }
}
