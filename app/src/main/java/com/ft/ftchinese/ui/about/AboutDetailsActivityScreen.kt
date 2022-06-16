package com.ft.ftchinese.ui.about

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.web.FtcWebView
import com.google.accompanist.web.rememberWebViewState

@Composable
fun AboutDetailsActivityScreen(
    url: String
) {
    val wvState = rememberWebViewState(url = url)

    ProgressLayout(
        loading = false,
        modifier = Modifier.fillMaxSize()
    ) {
        FtcWebView(wvState = wvState)
    }
}
