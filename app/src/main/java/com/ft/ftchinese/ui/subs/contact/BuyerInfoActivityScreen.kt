package com.ft.ftchinese.ui.subs.contact

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.store.FileStore
import com.ft.ftchinese.store.InvoiceStore
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.components.SimpleDialog
import com.ft.ftchinese.ui.components.rememberBaseUrl
import com.ft.ftchinese.ui.web.FtcWebView
import com.ft.ftchinese.viewmodel.UserViewModel
import com.google.accompanist.web.rememberWebViewStateWithHTMLData

@Composable
fun BuyerInfoActivityScreen(
    userViewModel: UserViewModel = viewModel(),
    scaffoldState: ScaffoldState,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val accountState = userViewModel.accountLiveData.observeAsState()
    val account = accountState.value
    if (account == null) {
        onExit()
        return
    }

    val invoiceStore = remember {
        InvoiceStore.getInstance(context)
    }
    val fileStore = remember {
        FileStore(context)
    }

    val baseUrl = rememberBaseUrl(account)

    val infoState = rememberBuyerInfoState(
        scaffoldState = scaffoldState
    )

    LaunchedEffect(key1 = Unit) {
        val action = invoiceStore.loadPurchaseAction()
        if (action == null) {
            onExit()
            return@LaunchedEffect
        }

        infoState.loadPage(
            account = account,
            action = action,
            cache = fileStore
        )
    }

    if (infoState.alert.isNotBlank()) {
        SimpleDialog(
            title = "",
            body = infoState.alert,
            onDismiss = infoState::closeAlert,
            onConfirm = infoState::closeAlert,
            dismissText = null,
        )
    }

    ProgressLayout(
        loading = infoState.progress.value,
        modifier = Modifier.fillMaxSize()
    ) {
        if (infoState.htmlLoaded.isNotBlank()) {
            FtcWebView(
                wvState = rememberWebViewStateWithHTMLData(
                    data = infoState.htmlLoaded,
                    baseUrl = baseUrl,
                ),
            )
        }
    }
}
