package com.ft.ftchinese.ui.account.stripewallet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.ui.components.CreateCustomerDialog
import com.ft.ftchinese.viewmodel.UserViewModel

@Composable
fun StripeSetupActivityScreen(
    walletViewModel: StripeWalletViewModel,
    userViewModel: UserViewModel = viewModel(),
    onExit: () -> Unit,
    showSnackBar: (String) -> Unit
) {

    val loading by walletViewModel.progressLiveData.observeAsState(false)
    val paymentMethod by walletViewModel.paymentMethodInUse.observeAsState()
    val customer by walletViewModel.customerLiveData.observeAsState()

    val accountState = userViewModel.accountLiveData.observeAsState()
    val account = accountState.value

    if (account == null) {
        showSnackBar("Not logged in")
        return
    }

    LaunchedEffect(key1 = customer) {
        customer?.let {
            userViewModel.saveAccount(account.withCustomerID(it.id))
        }
    }

    // Show dialog if user is not a stripe customer yet.
    // If user clicked cancel button, exit this activity.
    if (account.stripeId.isNullOrBlank()) {
        CreateCustomerDialog(
            email = account.email,
            onDismiss = onExit,
            onConfirm = {
                walletViewModel.createCustomer(account)
            }
        )
    }

    // Upon initial loading, fetch user's default payment method.
    LaunchedEffect(key1 = Unit) {
        walletViewModel.loadDefaultPaymentMethod(account)
    }

    StripeWalletScreen(
        loading = loading,
        paymentMethod = paymentMethod?.current,
        isDefault = paymentMethod?.isDefault ?: false,
        onSetDefault = {
            walletViewModel.setDefaultPaymentMethod(
                account = account,
                paymentMethod = it,
            )
        },
        onAddCard = {
            walletViewModel.showPaymentSheet(account)
        }
    )
}
