package com.ft.ftchinese.ui.about

import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.webpage.ChromeClient
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState

@Composable
fun AboutDetailsActivityScreen(
    url: String
) {
    val wvState = rememberWebViewState(url = url)

    ProgressLayout(
        loading = false
    ) {
        WebView(
            state = wvState,
            modifier = Modifier.fillMaxSize(),
            captureBackPresses = false,
            onCreated = {
                it.settings.javaScriptEnabled = true
                it.settings.loadsImagesAutomatically = true
                it.settings.domStorageEnabled = true
                it.settings.databaseEnabled = true
                it.webChromeClient = ChromeClient()
                it.webViewClient = WebViewClient()
            }
        )
    }
}
