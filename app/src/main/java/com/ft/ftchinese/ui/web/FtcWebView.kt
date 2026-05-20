package com.ft.ftchinese.ui.web

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ft.ftchinese.store.WebViewAccessTokenCookieManager
import com.google.accompanist.web.WebContent
import com.google.accompanist.web.WebView
import com.google.accompanist.web.WebViewState
import android.webkit.WebView as AndroidWebView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FtcWebView(
    wvState: WebViewState,
    modifier: Modifier = Modifier,
    initialUrl: String? = null,
    webClientCallback: WebViewCallback = rememberWebViewCallback(),
    jsListener: JsEventListener = rememberFtcJsEventListener(),
    onCreated: (AndroidWebView) -> Unit = {}
) {

    val fullscreenState = rememberFullscreenVideoState()
    val lifecycleOwner = LocalLifecycleOwner.current
    var activeWebView by remember { mutableStateOf<AndroidWebView?>(null) }

    val chromeClient = remember(fullscreenState) {
        FullscreenAccompanistChromeClient(fullscreenState)
    }

    val webClient = remember(webClientCallback) {
        FtcWebViewClient(
            callback = webClientCallback
        )
    }
    val authUrl = initialUrl ?: (wvState.content as? WebContent.Url)?.url

    Box(
        modifier = modifier
            .fillMaxWidth()
    ) {
        WebView(
            state = wvState,
            modifier = Modifier.fillMaxSize(),
            captureBackPresses = false,
            onCreated = { webView ->
                activeWebView = webView
                webView.settings.javaScriptEnabled = true
                webView.settings.loadsImagesAutomatically = true
                webView.settings.domStorageEnabled = true
                webView.settings.databaseEnabled = true
                WebViewAccessTokenCookieManager.syncAccessToken(webView)
                WebViewAccessTokenCookieManager.syncAccessTokenForUrl(webView, authUrl)
                webView.addJavascriptInterface(
                    JsInterface(
                        listener = jsListener,
                        onLink = { url ->
                            routeWebViewBridgeLink(
                                webView = webView,
                                callback = webClientCallback,
                                url = url,
                            )
                        },
                    ),
                    JS_INTERFACE_NAME
                )
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

    DisposableEffect(lifecycleOwner, activeWebView) {
        val webView = activeWebView
        if (webView == null) {
            onDispose {}
        } else {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> webView.pauseHtmlMedia()
                    Lifecycle.Event.ON_RESUME -> runCatching { webView.onResume() }
                    Lifecycle.Event.ON_DESTROY -> webView.stopHtmlMedia(clearPage = true)
                    else -> {}
                }
            }

            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                webView.stopHtmlMedia(clearPage = true)
                if (activeWebView === webView) {
                    activeWebView = null
                }
            }
        }
    }

    DisposableEffect(fullscreenState) {
        onDispose {
            fullscreenState.release()
        }
    }
}
