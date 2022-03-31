package com.ft.ftchinese.ui.subsactivity

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
import com.ft.ftchinese.ui.login.AuthActivity
import com.ft.ftchinese.ui.paywall.LinkEmailDialog
import com.ft.ftchinese.ui.paywall.PaywallScreen
import com.ft.ftchinese.ui.paywall.PaywallViewModel
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

@Composable
fun PaywallActivityScreen(
    paywallViewModel: PaywallViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel(),
    onFtcPay: (item: CartItemFtcV2) -> Unit,
    onStripePay: (item: CartItemStripeV2) -> Unit,
    onError: (String) -> Unit,
) {

    val context = LocalContext.current
    val isRefreshing by paywallViewModel.refreshingLiveData.observeAsState(false)

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

    LaunchedEffect(key1 = Unit) {
        paywallViewModel.loadPaywall(
            userViewModel.account?.isTest ?: false
        )
        paywallViewModel.loadStripePrices()
    }

    LaunchedEffect(key1 = paywallViewModel.msgId) {
        paywallViewModel.msgId?.let {
            onError(context.getString(it))
        }
    }

    LaunchedEffect(key1 = paywallViewModel.errMsg) {
        paywallViewModel.errMsg?.let {
            onError(it)
        }
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

    SwipeRefresh(
        state = rememberSwipeRefreshState(
            isRefreshing = isRefreshing,
        ),
        onRefresh = { paywallViewModel.refresh() },
    ) {
        PaywallScreen(
            paywall = paywallViewModel.paywallState,
            stripePrices = paywallViewModel.stripeState,
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
                if (!userViewModel.isWxOnly) {
                    setOpenDialog(true)
                    return@PaywallScreen
                }
                onStripePay(it)
            },
            onError = onError,
            onLoginRequest = {
                launchLoginActivity(launcher, context)
            },
        )
    }
}
