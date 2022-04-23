package com.ft.ftchinese.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.ui.base.JS_INTERFACE_NAME
import com.ft.ftchinese.ui.theme.OColor
import com.ft.ftchinese.ui.webpage.*
import com.google.accompanist.web.WebView
import com.google.accompanist.web.WebViewState

@Composable
fun WebPage(
    wvState: WebViewState,
    onJsEvent: (JsEvent) -> Unit,
    onWebViewEvent: (WVEvent) -> Unit,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
    ) {
        WebView(
            state = wvState,
            modifier = Modifier
                .fillMaxSize()
                .background(OColor.paper),
            captureBackPresses = false,
            onCreated = {
                it.settings.javaScriptEnabled = true
                it.settings.loadsImagesAutomatically = true
                it.settings.domStorageEnabled = true
                it.settings.databaseEnabled = true
                it.webChromeClient = ChromeClient()
                it.webViewClient = WVClientV2(
                    context = context,
                    onEvent = onWebViewEvent
                )
                it.addJavascriptInterface(
                    JsInterface(onJsEvent),
                    JS_INTERFACE_NAME,
                )
            }
        )
    }
}
