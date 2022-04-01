package com.ft.ftchinese.ui.subsactivity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.FetchUi
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.ui.paywall.PaywallViewModel
import com.ft.ftchinese.ui.components.CreateCustomerDialog
import com.ft.ftchinese.ui.stripepay.StripePayScreen
import com.ft.ftchinese.ui.stripepay.StripePayViewModel
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
    val (loading, setLoading) = remember {
        mutableStateOf(false)
    }
    val account = userViewModel.account
    val customerState = userViewModel.progressLiveData.observeAsState()
    val subsState = payViewModel.progressLiveData.observeAsState()

    if (account == null) {
        showSnackBar("Not logged in")
        return
    }

    if (priceId.isNullOrBlank()) {
        showSnackBar("Price id not passed in")
        return
    }

    when (val s = customerState.value) {
        is FetchUi.ResMsg -> {
            showSnackBar(context.getString(s.strId))
        }
        is FetchUi.TextMsg -> {
            showSnackBar(s.text)
        }
        is FetchUi.Progress -> {
            setLoading(s.loading)
        }
        else -> {}
    }

    when (val s = subsState.value) {
        is FetchUi.ResMsg -> {
            showSnackBar(context.getString(s.strId))
        }
        is FetchUi.TextMsg -> {
            showSnackBar(s.text)
        }
        is FetchUi.Progress -> {
            setLoading(s.loading)
        }
        else -> {}
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
            loading = loading,
            onSelectPayment = {  },
            onClickPay = {
                payViewModel.subscribe(
                    account = account,
                    item = item,
                )
            }
        )
    }
}
