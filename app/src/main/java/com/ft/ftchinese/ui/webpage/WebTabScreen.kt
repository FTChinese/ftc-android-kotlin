package com.ft.ftchinese.ui.webpage

import android.net.Uri
import android.webkit.WebView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.ui.components.CloseBar
import com.ft.ftchinese.ui.components.MenuOpenInBrowser
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.web.UrlHandler
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

    val context = LocalContext.current
    val wvState = rememberWebViewState(url = url)

    val webClient = remember {
        object : AccompanistWebViewClient() {
            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        CloseBar(
            onClose = onClose,
            title = title,
            actions = {
                if (openInBrowser) {
                    MenuOpenInBrowser {
                        UrlHandler.openInCustomTabs(
                            ctx = context,
                            url = Uri.parse(url)
                        )
                    }
                }
            }
        )

        ProgressLayout(
            loading = wvState.isLoading
        ) {
            WebView(
                state = wvState,
                modifier = Modifier
                    .fillMaxWidth(),
                captureBackPresses = true,
                onCreated = { webView ->
                    webView.settings.javaScriptEnabled = true
                    webView.settings.loadsImagesAutomatically = true
                    webView.settings.domStorageEnabled = true
                    webView.settings.databaseEnabled = true
                },
                client = webClient,
            )
        }
    }
}
