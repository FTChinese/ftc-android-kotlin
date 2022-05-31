package com.ft.ftchinese.ui.subs.invoice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.store.InvoiceStore
import com.ft.ftchinese.ui.base.toast
import com.ft.ftchinese.viewmodel.UserViewModel

@Composable
fun LatestInvoiceActivityScreen(
    userViewModel: UserViewModel = viewModel(),
    onNext: () -> Unit
) {
    val context = LocalContext.current
    val accountState = userViewModel.accountLiveData.observeAsState()
    val account = accountState.value
    if (account == null) {
        context.toast("Account not found")
        return
    }

    val invoiceStore = remember {
        InvoiceStore.getInstance(context)
    }

    LatestInvoiceScreen(
        invoices = invoiceStore.loadInvoices(),
        membership = account.membership,
        onClickNext = {
            onNext()
        }
    )
}
