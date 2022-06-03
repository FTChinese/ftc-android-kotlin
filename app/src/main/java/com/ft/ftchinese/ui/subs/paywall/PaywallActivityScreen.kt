package com.ft.ftchinese.ui.subs.paywall

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.model.paywall.CartItemFtc
import com.ft.ftchinese.model.paywall.CartItemStripe
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.tracking.AddCartParams
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.ui.auth.AuthActivity
import com.ft.ftchinese.ui.wxlink.launchWxLinkEmailActivity
import com.ft.ftchinese.viewmodel.UserViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

private fun launchLoginActivity(
    launcher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    context: Context,
) {
    launcher.launch(
        AuthActivity.newIntent(context)
    )
}

@Composable
fun PaywallActivityScreen(
    userViewModel: UserViewModel = viewModel(),
    scaffoldState: ScaffoldState,
    premiumOnTop: Boolean,
    onFtcPay: (item: CartItemFtc) -> Unit,
    onStripePay: (item: CartItemStripe) -> Unit,
) {

    val context = LocalContext.current
    val cache = remember {
        FileCache(context)
    }
    val tracker = remember {
        StatsTracker.getInstance(context)
    }

    val accountState = userViewModel.accountLiveData.observeAsState()
    val account = accountState.value

    val (openDialog, setOpenDialog) = remember {
        mutableStateOf(false)
    }

    val paywallState = rememberPaywallState(
        scaffoldState = scaffoldState
    )

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                userViewModel.reloadAccount()
            }
            Activity.RESULT_CANCELED -> {

            }
        }
    }

    LaunchedEffect(key1 = Unit) {
        paywallState.loadPaywall(
            isTest = account?.isTest ?: false,
            cache = cache
        )

        tracker.displayPaywall()
    }

    if (openDialog ) {
        LinkEmailDialog(
            onConfirm = {
                launchWxLinkEmailActivity(launcher, context)
                setOpenDialog(false)
            },
            onDismiss = {
                setOpenDialog(false)
            }
        )
    }

    SwipeRefresh(
        state = rememberSwipeRefreshState(
            isRefreshing = paywallState.refreshing,
        ),
        onRefresh = {
            paywallState.refreshPaywall(
                isTest = account?.isTest ?: false,
                cache = cache
            )
        },
    ) {
        PaywallScreen(
            paywall = paywallState.paywallData
                .reOrderProducts(premiumOnTop),
            account = account,
            onFtcPay = {
                if (!userViewModel.isLoggedIn) {
                    launchLoginActivity(launcher, context)
                    return@PaywallScreen
                }

                tracker.addCart(AddCartParams.ofFtc(it.price))
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
                tracker.addCart(AddCartParams.ofStripe(it.recurring))
                onStripePay(it)
            },
        ) {
            launchLoginActivity(launcher, context)
        }
    }
}
