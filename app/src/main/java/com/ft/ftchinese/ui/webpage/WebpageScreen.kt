package com.ft.ftchinese.ui.webpage

import android.webkit.WebView
import androidx.compose.runtime.Composable
import com.ft.ftchinese.model.content.WebpageMeta
import com.ft.ftchinese.ui.web.FtcWebView
import com.google.accompanist.web.rememberWebViewState

@Composable
fun WebpageScreen(
    pageMeta: WebpageMeta,
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
            onCreated = onWebViewCreated,
        )
    }
}
