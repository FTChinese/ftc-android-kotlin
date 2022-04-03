package com.ft.ftchinese.ui.stripepay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.ui.components.CreateCustomerDialog
import com.ft.ftchinese.ui.paywall.PaywallViewModel
import com.ft.ftchinese.viewmodel.UserViewModel

@Composable
fun StripePayActivityScreen(
    userViewModel: UserViewModel = viewModel(),
    payViewModel: StripePayViewModel = viewModel(),
    pwViewModel: PaywallViewModel,
    priceId: String?,
    trialId: String?,
    showSnackBar: (String) -> Unit,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val account = userViewModel.account
    val inProgress by payViewModel.inProgress.observeAsState(false)
    val status by payViewModel.status.observeAsState("")

    if (account == null) {
        showSnackBar("Not logged in")
        return
    }

    if (priceId.isNullOrBlank()) {
        showSnackBar("Price id not passed in")
        return
    }

    if (status.isNotBlank()) {
        showSnackBar(status)
    }

    if (account.stripeId.isNullOrBlank()) {
        CreateCustomerDialog(
            email = account.email,
            onDismiss = onExit,
            onConfirm = {
                if (!context.isConnected) {
                    showSnackBar(context.getString(R.string.prompt_no_network))
                    return@CreateCustomerDialog
                }

                userViewModel.createCustomer(account)
            }
        )
    }

    val item = pwViewModel.stripeCheckoutItem(
        priceId = priceId,
        trialId = trialId,
        m = account.membership.normalize()
    )

    if (item != null) {
        StripePayScreen(
            cartItem = item,
            loading = inProgress,
            paymentMethod = null,
            subs = null,
            onPaymentMethod = {
                payViewModel.createSetupIntent(account)
            },
            onSubscribe = {
                payViewModel.subscribe(
                    account = account,
                    item = item,
                )
            },
            onDone = {}
        )
    }
}
