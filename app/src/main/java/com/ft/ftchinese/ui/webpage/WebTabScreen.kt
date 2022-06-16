package com.ft.ftchinese.ui.webpage

import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState

@Composable
fun WebTabScreen(
    url: String,
    title: String = "",
    openInBrowser: Boolean = false,
    onClose: () -> Unit,
) {

    val wvState = rememberWebViewState(url = url)

    val webClient = remember {
        object : AccompanistWebViewClient() {
            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
            }
        }
    }

    WebContentLayout(
        url = if (openInBrowser) {
            url
         } else { null },
        title = title,
        loading = wvState.isLoading,
        onClose = onClose
    ) {
        WebView(
            state = wvState,
            modifier = Modifier
                .fillMaxWidth(),
            captureBackPresses = true,
            onCreated = { webView ->
                webView.settings.javaScriptEnabled = true
                webView.settings.loadsImagesAutomatically = true
                webView.settings.domStorageEnabled = true
                webView.settings.databaseEnabled = true
            },
            client = webClient,
        )
    }
}
