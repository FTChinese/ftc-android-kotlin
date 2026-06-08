package com.ft.ftchinese.ui.webpage

import android.graphics.Bitmap
import android.util.Log
import android.webkit.WebResourceRequest
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.ft.ftchinese.repository.HostConfig
import com.ft.ftchinese.store.WebViewAccessTokenCookieManager
import com.ft.ftchinese.ui.web.FullscreenAccompanistChromeClient
import com.ft.ftchinese.ui.web.JS_INTERFACE_NAME
import com.ft.ftchinese.ui.web.JsInterface
import com.ft.ftchinese.ui.web.rememberFtcJsEventListener
import com.ft.ftchinese.ui.web.rememberFullscreenVideoState
import com.ft.ftchinese.ui.web.rememberWebViewCallback
import com.ft.ftchinese.ui.web.routeWebViewBridgeLink
import com.ft.ftchinese.ui.web.WebViewUnavailable
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.WebContent
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState
import android.webkit.WebView as AndroidWebView

private const val TAG = "WebTabScreen"

@Composable
fun WebTabScreen(
    url: String,
    title: String = "",
    openInBrowser: Boolean = false,
    requestHeaders: Map<String, String> = emptyMap(),
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val wvState = rememberWebViewState(
        url = url,
        additionalHttpHeaders = requestHeaders
    )
    val hasClearedOneTimeHeaders = remember(url, requestHeaders) {
        mutableStateOf(requestHeaders.isEmpty())
    }
    val jsListener = rememberFtcJsEventListener()
    val webViewCallback = rememberWebViewCallback()

    val fullscreenState = rememberFullscreenVideoState()

    val safeWebView = remember(context) {
        try {
            AndroidWebView(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create native WebView", e)
            null
        }
    }

    val webClient = remember(url, requestHeaders) {
        object : AccompanistWebViewClient() {
            override fun onPageStarted(view: AndroidWebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                WebViewAccessTokenCookieManager.syncAccessTokenForUrl(view, url)

                // Keep auth headers for the initial bootstrap request only.
                if (!hasClearedOneTimeHeaders.value && requestHeaders.isNotEmpty()) {
                    hasClearedOneTimeHeaders.value = true
                    val currentUrl = url ?: (wvState.content as? WebContent.Url)?.url ?: return
                    wvState.content = WebContent.Url(currentUrl, emptyMap())
                }
            }

            override fun doUpdateVisitedHistory(view: AndroidWebView?, url: String?, isReload: Boolean) {
            }

            override fun shouldOverrideUrlLoading(view: AndroidWebView?, request: WebResourceRequest?): Boolean {
                WebViewAccessTokenCookieManager.syncAccessTokenForUrl(
                    view,
                    request?.url?.toString()
                )
                return super.shouldOverrideUrlLoading(view, request)
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
            val nativeWebView = safeWebView
            if (nativeWebView == null) {
                WebViewUnavailable(
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                WebView(
                    state = wvState,
                    modifier = Modifier.fillMaxSize(),
                    captureBackPresses = true,
                    factory = { nativeWebView },
                    onCreated = { createdWebView ->
                        createdWebView.settings.javaScriptEnabled = true
                        createdWebView.settings.loadsImagesAutomatically = true
                        createdWebView.settings.domStorageEnabled = true
                        createdWebView.settings.databaseEnabled = true
                        if (shouldPrepareFtWebViewAuth(url)) {
                            WebViewAccessTokenCookieManager.syncAccessToken(createdWebView)
                            WebViewAccessTokenCookieManager.syncAccessTokenForUrl(createdWebView, url)
                            createdWebView.addJavascriptInterface(
                                JsInterface(
                                    listener = jsListener,
                                    onLink = { link ->
                                        routeWebViewBridgeLink(
                                            webView = createdWebView,
                                            callback = webViewCallback,
                                            url = link,
                                        )
                                    },
                                ),
                                JS_INTERFACE_NAME
                            )
                        }
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

private fun shouldPrepareFtWebViewAuth(url: String): Boolean {
    return HostConfig.trustedAuthOrigin(url) != null
}
