package com.ft.ftchinese.ui.webpage

import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebView
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
import androidx.compose.ui.viewinterop.AndroidView
import com.ft.ftchinese.repository.HostConfig
import com.ft.ftchinese.store.WebViewAccessTokenCookieManager
import com.ft.ftchinese.ui.web.FullscreenAccompanistChromeClient
import com.ft.ftchinese.ui.web.JS_INTERFACE_NAME
import com.ft.ftchinese.ui.web.JsInterface
import com.ft.ftchinese.ui.web.rememberFtcJsEventListener
import com.ft.ftchinese.ui.web.rememberFullscreenVideoState
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.WebContent
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState

@Composable
fun WebTabScreen(
    url: String,
    title: String = "",
    openInBrowser: Boolean = false,
    requestHeaders: Map<String, String> = emptyMap(),
    onClose: () -> Unit,
) {
    val wvState = rememberWebViewState(
        url = url,
        additionalHttpHeaders = requestHeaders
    )
    val hasClearedOneTimeHeaders = remember(url, requestHeaders) {
        mutableStateOf(requestHeaders.isEmpty())
    }
    val jsListener = rememberFtcJsEventListener()

    val fullscreenState = rememberFullscreenVideoState()

    val webClient = remember(url, requestHeaders) {
        object : AccompanistWebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)

                // Keep auth headers for the initial bootstrap request only.
                if (!hasClearedOneTimeHeaders.value && requestHeaders.isNotEmpty()) {
                    hasClearedOneTimeHeaders.value = true
                    val currentUrl = url ?: (wvState.content as? WebContent.Url)?.url ?: return
                    wvState.content = WebContent.Url(currentUrl, emptyMap())
                }
            }

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
                    if (shouldPrepareFtWebViewAuth(url)) {
                        WebViewAccessTokenCookieManager.syncAccessToken(webView)
                        webView.addJavascriptInterface(
                            JsInterface(jsListener),
                            JS_INTERFACE_NAME
                        )
                    }
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

private fun shouldPrepareFtWebViewAuth(url: String): Boolean {
    val host = Uri.parse(url).host ?: return false
    return host == HostConfig.HOST_AI_CHAT || HostConfig.isInternalLink(host)
}
