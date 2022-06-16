package com.ft.ftchinese.ui.webpage

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.model.legal.WebpageMeta
import com.ft.ftchinese.ui.components.CloseBar
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.web.FtcWebView
import com.ft.ftchinese.ui.web.UrlHandler
import com.google.accompanist.web.rememberWebViewState

@Composable
fun WebpageScreen(
    pageMeta: WebpageMeta,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val webViewState = rememberWebViewState(url = pageMeta.url)

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        CloseBar(
            onClose = onExit,
            title = pageMeta.title,
            actions = {
                if (pageMeta.showMenu) {
                    UrlHandler.openInCustomTabs(
                        ctx = context,
                        url = Uri.parse(pageMeta.url)
                    )
                }
            }
        )

        ProgressLayout(
            loading = webViewState.isLoading
        ) {
            FtcWebView(
                wvState = webViewState,
            )
        }
    }

}
