package com.ft.ftchinese.ui.paywall

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.model.paywall.CartItemFtcV2
import com.ft.ftchinese.model.paywall.CartItemStripeV2
import com.ft.ftchinese.model.paywall.defaultPaywall
import com.ft.ftchinese.ui.checkout.StripeSubActivity
import com.ft.ftchinese.ui.components.ToastMessage
import com.ft.ftchinese.ui.login.AuthActivity
import com.ft.ftchinese.ui.wxlink.LinkFtcActivity
import com.ft.ftchinese.viewmodel.UserViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

private fun launchLoginActivity(
    launcher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    context: Context,
) {
    launcher.launch(
        AuthActivity.intent(context)
    )
}

private fun launchLinkFtcActivity(
    launcher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    context: Context,
) {
    launcher.launch(LinkFtcActivity.intent(context))
}

private fun launchStripeSubsActivity(
    launcher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    context: Context,
    item: CartItemStripeV2,
) {
    launcher.launch(StripeSubActivity.intent(context, item))
}

@Composable
fun PaywallActivityScreen(
    paywallViewModel: PaywallViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel(),
    onFtcPay: (item: CartItemFtcV2) -> Unit,
//    onStripePay: (item: CartItemStripeV2) -> Unit,
    showSnackBar: (String) -> Unit,
) {

    val context = LocalContext.current
    val isRefreshing by paywallViewModel.refreshingLiveData.observeAsState(false)
    val ftcPaywall by paywallViewModel.ftcPriceLiveData.observeAsState(defaultPaywall)
    val stripePrices by paywallViewModel.stripePriceLiveData.observeAsState(mapOf())
    val toastState = paywallViewModel.toastLiveData.observeAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                userViewModel.load()
            }
            Activity.RESULT_CANCELED -> {

            }
        }
    }

    val (openDialog, setOpenDialog) = remember {
        mutableStateOf(false)
    }

    when (val s = toastState.value) {
        is ToastMessage.Resource -> {
            showSnackBar(context.getString(s.id))
        }
        is ToastMessage.Text -> {
            showSnackBar(s.text)
        }
        else -> {}
    }

    if (openDialog ) {
        LinkEmailDialog(
            onConfirm = {
                launchLinkFtcActivity(launcher, context)
                setOpenDialog(false)
            },
            onDismiss = {
                setOpenDialog(false)
            }
        )
    }

    LaunchedEffect(key1 = Unit) {
        paywallViewModel.loadPaywall(
            userViewModel.account?.isTest ?: false
        )
        paywallViewModel.loadStripePrices()
    }

    SwipeRefresh(
        state = rememberSwipeRefreshState(
            isRefreshing = isRefreshing,
        ),
        onRefresh = { paywallViewModel.refresh() },
    ) {
        PaywallScreen(
            paywall = ftcPaywall,
            stripePrices = stripePrices,
            account = userViewModel.account,
            onFtcPay = {
                if (!userViewModel.isLoggedIn) {
                    launchLoginActivity(launcher, context)
                    return@PaywallScreen
                }
                onFtcPay(it)
            },
            onStripePay = {
                if (!userViewModel.isLoggedIn) {
                    launchLoginActivity(launcher, context)
                    return@PaywallScreen
                }
                if (userViewModel.isWxOnly) {
                    setOpenDialog(true)
                    return@PaywallScreen
                }
//                onStripePay(it)
                launchStripeSubsActivity(launcher, context, it)
            },
            onError = showSnackBar,
            onLoginRequest = {
                launchLoginActivity(launcher, context)
            },
        )
    }
}
