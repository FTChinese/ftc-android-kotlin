package com.ft.ftchinese.ui.web

import android.annotation.SuppressLint
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ft.ftchinese.store.WebViewAccessTokenCookieManager
import com.google.accompanist.web.WebContent
import com.google.accompanist.web.WebView
import com.google.accompanist.web.WebViewState
import android.webkit.WebView as AndroidWebView

private const val TAG = "FtcWebView"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FtcWebView(
    wvState: WebViewState,
    modifier: Modifier = Modifier,
    initialUrl: String? = null,
    webClientCallback: WebViewCallback = rememberWebViewCallback(),
    jsListener: JsEventListener = rememberFtcJsEventListener(),
    captureBackPresses: Boolean = false,
    pauseMediaOnLifecyclePause: Boolean = true,
    onPageStarted: (AndroidWebView?, String?) -> Unit = { _, _ -> },
    onCreated: (AndroidWebView) -> Unit = {}
) {

    val context = LocalContext.current
    val fullscreenState = rememberFullscreenVideoState()
    val lifecycleOwner = LocalLifecycleOwner.current
    var activeWebView by remember { mutableStateOf<AndroidWebView?>(null) }
    val safeWebView = remember(context) {
        try {
            AndroidWebView(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create native WebView", e)
            null
        }
    }

    val chromeClient = remember(fullscreenState) {
        FullscreenAccompanistChromeClient(fullscreenState)
    }

    val webClient = remember(webClientCallback) {
        FtcWebViewClient(
            callback = webClientCallback,
            onPageStartedCallback = onPageStarted,
        )
    }
    val authUrl = initialUrl ?: (wvState.content as? WebContent.Url)?.url

    Box(
        modifier = modifier
            .fillMaxWidth()
    ) {
        val nativeWebView = safeWebView
        if (nativeWebView == null) {
            WebViewUnavailable(
                modifier = Modifier.fillMaxSize()
            )
        } else {
            WebView(
                state = wvState,
                modifier = Modifier.fillMaxSize(),
                captureBackPresses = captureBackPresses,
                factory = { nativeWebView },
                onCreated = { createdWebView ->
                    activeWebView = createdWebView
                    createdWebView.settings.javaScriptEnabled = true
                    createdWebView.settings.loadsImagesAutomatically = true
                    createdWebView.settings.domStorageEnabled = true
                    createdWebView.settings.databaseEnabled = true
                    WebViewAccessTokenCookieManager.syncAccessToken(createdWebView)
                    WebViewAccessTokenCookieManager.syncAccessTokenForUrl(createdWebView, authUrl)
                    createdWebView.addJavascriptInterface(
                        JsInterface(
                            listener = jsListener,
                            onLink = { url ->
                                routeWebViewBridgeLink(
                                    webView = createdWebView,
                                    callback = webClientCallback,
                                    url = url,
                                )
                            },
                        ),
                        JS_INTERFACE_NAME
                    )
                    onCreated(createdWebView)
                },
                client = webClient,
                chromeClient = chromeClient,
            )
        }

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

    DisposableEffect(lifecycleOwner, activeWebView, pauseMediaOnLifecyclePause) {
        val webView = activeWebView
        if (webView == null) {
            onDispose {}
        } else {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> {
                        if (pauseMediaOnLifecyclePause) {
                            webView.pauseHtmlMedia()
                        }
                    }
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

@Composable
fun WebViewUnavailable(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("网页组件不可用，请更新 Android System WebView 或 Chrome 后重试")
    }
}
