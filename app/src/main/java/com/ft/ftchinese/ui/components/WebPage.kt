package com.ft.ftchinese.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ft.ftchinese.ui.base.JS_INTERFACE_NAME
import com.ft.ftchinese.ui.webpage.ChromeClient
import com.ft.ftchinese.ui.webpage.WVClient
import com.google.accompanist.web.WebView
import com.google.accompanist.web.WebViewState

@Composable
fun WebPage(
    loading: Boolean,
    state: WebViewState,
    wvClient: WVClient,
    jsInterface: WebClientViewModel,
) {
    ProgressLayout(
        loading = loading
    ) {
        WebView(
            state = state,
            modifier = Modifier.fillMaxSize(),
            captureBackPresses = false,
            onCreated = {
                it.settings.javaScriptEnabled = true
                it.settings.loadsImagesAutomatically = true
                it.settings.domStorageEnabled = true
                it.settings.databaseEnabled = true
                it.webChromeClient = ChromeClient()
                it.webViewClient = wvClient
                it.addJavascriptInterface(jsInterface, JS_INTERFACE_NAME)
            }
        )
    }
}
