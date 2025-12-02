package com.ft.ftchinese.ui.webpage

import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.ft.ftchinese.ui.web.FullscreenAccompanistChromeClient
import com.ft.ftchinese.ui.web.rememberFullscreenVideoState
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

    val fullscreenState = rememberFullscreenVideoState()

    val webClient = remember {
        object : AccompanistWebViewClient() {
            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
            }
        }
    }

    val chromeClient = remember(fullscreenState) {
        FullscreenAccompanistChromeClient(fullscreenState)
    }

    WebContentLayout(
        url = if (openInBrowser) {
            url
         } else { null },
        title = title,
        loading = wvState.isLoading,
        onClose = onClose
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            WebView(
                state = wvState,
                modifier = Modifier.fillMaxSize(),
                captureBackPresses = true,
                onCreated = { webView ->
                    webView.settings.javaScriptEnabled = true
                    webView.settings.loadsImagesAutomatically = true
                    webView.settings.domStorageEnabled = true
                    webView.settings.databaseEnabled = true
                },
                client = webClient,
                chromeClient = chromeClient,
            )

            fullscreenState.customView?.let { customView ->
                AndroidView(
                    factory = {
                        (customView.parent as? android.view.ViewGroup)?.removeView(customView)
                        customView
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )
            }
        }
    }

    BackHandler(enabled = fullscreenState.isFullscreen) {
        fullscreenState.hide()
    }

    DisposableEffect(fullscreenState) {
        onDispose {
            fullscreenState.release()
        }
    }
}
