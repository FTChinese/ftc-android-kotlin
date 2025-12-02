package com.ft.ftchinese.ui.web

import android.annotation.SuppressLint
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
import com.google.accompanist.web.WebView
import com.google.accompanist.web.WebViewState

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FtcWebView(
    wvState: WebViewState,
    modifier: Modifier = Modifier,
    webClientCallback: WebViewCallback = rememberWebViewCallback(),
    jsListener: JsEventListener = rememberFtcJsEventListener(),
    onCreated: (WebView) -> Unit = {}
) {

    val fullscreenState = rememberFullscreenVideoState()

    val chromeClient = remember(fullscreenState) {
        FullscreenAccompanistChromeClient(fullscreenState)
    }

    val jsInterface = remember(jsListener) {
        JsInterface(jsListener)
    }

    val webClient = remember(webClientCallback) {
        FtcWebViewClient(
            callback = webClientCallback
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
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

    BackHandler(enabled = fullscreenState.isFullscreen) {
        fullscreenState.hide()
    }

    DisposableEffect(fullscreenState) {
        onDispose {
            fullscreenState.release()
        }
    }
}
