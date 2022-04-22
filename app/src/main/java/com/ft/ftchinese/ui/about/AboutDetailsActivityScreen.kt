package com.ft.ftchinese.ui.about

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.ui.components.WebInterfaceViewModel
import com.ft.ftchinese.ui.components.WebPage
import com.ft.ftchinese.ui.webpage.WVClient
import com.google.accompanist.web.rememberWebViewState

@Composable
fun AboutDetailsActivityScreen(
    url: String,
    webViewModel: WebInterfaceViewModel
) {
    val context = LocalContext.current
    val wvState = rememberWebViewState(url = url)
    val wvClient = WVClient(context)

    WebPage(
        loading = false,
        state = wvState,
        wvClient = wvClient,
        jsInterface = webViewModel,
    )
}
