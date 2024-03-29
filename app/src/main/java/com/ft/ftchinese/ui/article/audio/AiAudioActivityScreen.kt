package com.ft.ftchinese.ui.article.audio

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.ui.article.NavStore
import com.ft.ftchinese.ui.util.toast
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.util.UriUtils
import com.ft.ftchinese.ui.web.FtcWebView
import com.ft.ftchinese.viewmodel.UserViewModel
import com.google.accompanist.web.rememberWebViewState

@Composable
fun AiAudioActivityScreen(
    userViewModel: UserViewModel = viewModel(),
    id: String?
) {
    val context = LocalContext.current
    val accountState = userViewModel.accountLiveData.observeAsState()

    if (id == null) {
        context.toast("Missing id")
        return
    }

    val url = remember(accountState.value) {
        NavStore
            .getTeaser(id)
            ?.let {
                UriUtils.teaserUrl(it, accountState.value)
            }
    }

    if (url.isNullOrBlank()) {
        context.toast("Missing required url")
        return
    }

    val webViewState = rememberWebViewState(url = url)

    ProgressLayout(
        loading = webViewState.isLoading,
        modifier = Modifier.fillMaxSize()
    ) {
        FtcWebView(
            wvState = webViewState,
        )
    }

}
