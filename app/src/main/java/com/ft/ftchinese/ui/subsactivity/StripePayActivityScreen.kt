package com.ft.ftchinese.ui.subsactivity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.ui.paywall.PaywallViewModel
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
    showSnackBar: (String) -> Unit
) {
    val context = LocalContext.current
    val (loading, setLoading) = remember {
        mutableStateOf(false)
    }

    val account = userViewModel.account
    if (account == null) {
        showSnackBar("Not logged in")
        return
    }

    if (priceId.isNullOrBlank()) {
        showSnackBar("Price id not passed in")
        return
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
