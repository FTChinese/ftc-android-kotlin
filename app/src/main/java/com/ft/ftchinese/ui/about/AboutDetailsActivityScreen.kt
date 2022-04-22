package com.ft.ftchinese.ui.about

import androidx.compose.runtime.Composable
import com.ft.ftchinese.ui.components.WebClientViewModel
import com.ft.ftchinese.ui.components.WebPage
import com.ft.ftchinese.ui.webpage.WVClient
import com.google.accompanist.web.rememberWebViewState

@Composable
fun AboutDetailsActivityScreen(
    url: String,
    webViewModel: WebClientViewModel
) {
    val wvState = rememberWebViewState(url = url)
    val wvClient = WVClient()

    WebPage(
        loading = false,
        state = wvState,
        wvClient = wvClient,
        jsInterface = webViewModel,
    )
}
