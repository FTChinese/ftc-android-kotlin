package com.ft.ftchinese.ui.subs.paywall

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.R
import com.ft.ftchinese.model.paywall.CartItemFtc
import com.ft.ftchinese.model.paywall.CartItemStripe
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.repository.ApiConfig
import com.ft.ftchinese.tracking.AddCartParams
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.ui.auth.AuthActivity
import com.ft.ftchinese.ui.util.AccountAction
import com.ft.ftchinese.ui.util.IntentsUtil
import com.ft.ftchinese.ui.util.toast
import com.ft.ftchinese.ui.wxlink.launchWxLinkEmailActivity
import com.ft.ftchinese.viewmodel.UserViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@Composable
fun PaywallActivityScreen(
    userViewModel: UserViewModel = viewModel(),
    scaffoldState: ScaffoldState,
    premiumOnTop: Boolean,
    onFtcPay: (item: CartItemFtc) -> Unit,
    onStripePay: (item: CartItemStripe) -> Unit,
) {

    val context = LocalContext.current
    val tracker = remember {
        StatsTracker.getInstance(context)
    }

    val accountState = userViewModel.accountLiveData.observeAsState()
    val account = accountState.value
    val isLoggedIn by userViewModel.loggedInLiveData.observeAsState(false)
    val isTest by userViewModel.testUserLiveData.observeAsState(false)

    val apiConfig = remember(key1 = isTest) {
        ApiConfig.ofSubs(isTest)
    }

    val (openDialog, setOpenDialog) = remember {
        mutableStateOf(false)
    }

    val paywallState = rememberPaywallState(
        scaffoldState = scaffoldState
    )

    // Launcher if user is not logged in.
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                userViewModel.reloadAccount()
                result.data?.let(IntentsUtil::getAccountAction)?.let {
                    if (it == AccountAction.SignedIn) {
                        context.toast(R.string.login_success)
                    }
                }
            }
            Activity.RESULT_CANCELED -> {

            }
        }
    }

    // Load paywall.
    LaunchedEffect(key1 = Unit) {
        paywallState.loadPaywall(
            api = apiConfig,
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

    val pwData = remember(paywallState.paywallData, premiumOnTop) {
        paywallState.paywallData
            .reOrderProducts(premiumOnTop)
    }

    SwipeRefresh(
        state = rememberSwipeRefreshState(
            isRefreshing = paywallState.refreshing,
        ),
        onRefresh = {
            paywallState.refreshPaywall(
                api = apiConfig,
            )
        },
    ) {
        PaywallScreen(
            paywall = pwData,
            membership = account?.membership?.normalize() ?: Membership(),
            isLoggedIn = isLoggedIn,
            onFtcPay = {
                if (!userViewModel.isLoggedIn) {
                    AuthActivity.launch(
                        launcher,
                        context,
                    )
                    return@PaywallScreen
                }

                tracker.addCart(AddCartParams.ofFtc(it.price))
                onFtcPay(it)
            },
            onStripePay = {
                if (!userViewModel.isLoggedIn) {
                    AuthActivity.launch(launcher, context)
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
            AuthActivity.launch(launcher, context)
        }
    }
}
