package com.ft.ftchinese.ui.webpage

import android.webkit.WebView
import androidx.compose.runtime.Composable
import com.ft.ftchinese.model.content.WebpageMeta
import com.ft.ftchinese.ui.web.FtcWebView
import com.ft.ftchinese.ui.web.WebViewCallback
import com.google.accompanist.web.rememberWebViewState

@Composable
fun WebpageScreen(
    pageMeta: WebpageMeta,
    webViewCallback: WebViewCallback? = null,
    onPageStarted: (WebView?, String?) -> Unit = { _, _ -> },
    onWebViewCreated: (WebView) -> Unit = {},
    onClose: () -> Unit,
) {
    val webViewState = rememberWebViewState(url = pageMeta.url)

    WebContentLayout(
        url = if (pageMeta.showMenu) {
            pageMeta.url
        } else { null },
        title = pageMeta.title,
        loading = webViewState.isLoading,
        useCloseButton = pageMeta.useCloseButton,
        onClose = onClose
    ) {
        FtcWebView(
            wvState = webViewState,
            initialUrl = pageMeta.url,
            webClientCallback = webViewCallback
                ?: com.ft.ftchinese.ui.web.rememberWebViewCallback(),
            onPageStarted = onPageStarted,
            onCreated = onWebViewCreated,
        )
    }
}
