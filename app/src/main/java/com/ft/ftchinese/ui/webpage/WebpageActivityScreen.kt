package com.ft.ftchinese.ui.webpage

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.model.legal.WebpageMeta
import com.ft.ftchinese.ui.components.MenuOpenInBrowser
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.components.Toolbar
import com.ft.ftchinese.ui.theme.OTheme
import com.ft.ftchinese.ui.web.ComposeWebView
import com.ft.ftchinese.ui.web.rememberWebViewClient
import com.google.accompanist.web.rememberWebViewState

@Composable
fun WebpageActivityScreen(
    pageMeta: WebpageMeta,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val scaffoldState = rememberScaffoldState()
    val webViewState = rememberWebViewState(url = pageMeta.url)

    OTheme {
        Scaffold(
            topBar = {
                Toolbar(
                    heading = pageMeta.title,
                    onBack = onExit,
                    actions = {
                        if (pageMeta.showMenu) {
                            MenuOpenInBrowser {
                                CustomTabsIntent.Builder()
                                    .build()
                                    .launchUrl(
                                        context,
                                        Uri.parse(pageMeta.url)
                                    )
                            }
                        }
                    }
                )
            },
            scaffoldState = scaffoldState
        ) {

            ProgressLayout(
                loading = webViewState.isLoading
            ) {
                ComposeWebView(
                    wvState = webViewState,
                    webClient = rememberWebViewClient()
                )
            }
        }
    }
}
