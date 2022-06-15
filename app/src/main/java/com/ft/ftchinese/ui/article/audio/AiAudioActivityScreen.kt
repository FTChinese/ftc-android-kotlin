package com.ft.ftchinese.ui.article.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.ui.base.toast
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.web.ComposeWebView
import com.ft.ftchinese.viewmodel.UserViewModel
import com.google.accompanist.web.rememberWebViewState

@Composable
fun AiAudioActivityScreen(
    userViewModel: UserViewModel = viewModel(),
) {
    val context = LocalContext.current

    val url = remember(userViewModel.account) {
        AudioTeaserStore
            .load()
            ?.htmlUrl(userViewModel.account)
    }

    if (url.isNullOrBlank()) {
        context.toast("Missing required url")
        return
    }

    val webViewState = rememberWebViewState(url = url)

    ProgressLayout(
        loading = webViewState.isLoading
    ) {
        ComposeWebView(
            wvState = webViewState,
        )
    }

}
