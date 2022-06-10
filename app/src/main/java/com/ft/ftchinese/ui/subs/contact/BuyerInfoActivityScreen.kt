package com.ft.ftchinese.ui.subs.contact

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.repository.Config
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.store.InvoiceStore
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.components.SimpleDialog
import com.ft.ftchinese.ui.web.ComposeWebView
import com.ft.ftchinese.ui.web.rememberWebViewClient
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
    val fileCache = remember {
        FileCache(context)
    }

    val baseUrl = remember(account) {
        Config.discoverServer(account)
    }

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
            cache = fileCache
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
    ) {
        if (infoState.htmlLoaded.isNotBlank()) {
            ComposeWebView(
                wvState = rememberWebViewStateWithHTMLData(
                    data = infoState.htmlLoaded,
                    baseUrl = baseUrl,
                ),
                webClient = rememberWebViewClient()
            )
        }
    }
}
