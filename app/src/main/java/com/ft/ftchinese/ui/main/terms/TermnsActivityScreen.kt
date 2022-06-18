package com.ft.ftchinese.ui.main.terms

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.model.content.legalPages
import com.ft.ftchinese.store.ServiceAcceptance
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.web.FtcWebView
import com.google.accompanist.web.rememberWebViewState

@Composable
fun TermsActivityScreen(
    onAgreed: () -> Unit,
    onDeclined: () -> Unit,
) {
    val context = LocalContext.current

    val agreement = remember {
        ServiceAcceptance.getInstance(context)
    }

    val wvState = rememberWebViewState(
        url = legalPages[1].url
    )

    ProgressLayout(
        loading = wvState.isLoading,
        modifier = Modifier.fillMaxSize()
    ) {
        TermsScreen(
            onAgree = {
                agreement.accept()
                onAgreed()
            },
            onDecline = onDeclined
        ) {
            FtcWebView(wvState = wvState)
        }
    }
}
