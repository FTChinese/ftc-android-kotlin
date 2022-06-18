package com.ft.ftchinese.ui.webpage

import androidx.compose.runtime.Composable
import com.ft.ftchinese.model.content.WebpageMeta
import com.ft.ftchinese.ui.web.FtcWebView
import com.google.accompanist.web.rememberWebViewState

@Composable
fun WebpageScreen(
    pageMeta: WebpageMeta,
    onClose: () -> Unit
) {
    val webViewState = rememberWebViewState(url = pageMeta.url)

    WebContentLayout(
        url = if (pageMeta.showMenu) {
            pageMeta.url
        } else { null },
        title = pageMeta.title,
        loading = webViewState.isLoading,
        onClose = onClose
    ) {
        FtcWebView(
            wvState = webViewState,
        )
    }
}
