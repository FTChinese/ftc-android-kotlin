package com.ft.ftchinese.ui.subs.member

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.ui.subs.SubsActivity
import com.ft.ftchinese.ui.util.AccountAction
import com.ft.ftchinese.ui.util.IntentsUtil
import com.ft.ftchinese.viewmodel.UserViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@Composable
fun MemberActivityScreen(
    modifier: Modifier = Modifier,
    userViewModel: UserViewModel = viewModel(),
    scaffoldState: ScaffoldState,
    showSnackBar: (String) -> Unit,
    onRefreshed: () -> Unit,
) {

    val context = LocalContext.current

    val memberState = rememberMembershipState(
        scaffoldState = scaffoldState
    )

    val accountState = userViewModel.accountLiveData.observeAsState()
    val account = accountState.value
    val (showDialog, setShowDialog) = remember {
        mutableStateOf(false)
    }

    LaunchedEffect(key1 = memberState.accountUpdated) {
        memberState.accountUpdated?.let {
            userViewModel.saveAccount(it)
            onRefreshed()
        }
    }

    LaunchedEffect(key1 = memberState.stripeSubsUpdated) {
        memberState.stripeSubsUpdated?.let {
            userViewModel.saveStripeSubs(it)
            onRefreshed()
        }
    }

    LaunchedEffect(key1 = memberState.iapSubsUpdated) {
        memberState.iapSubsUpdated?.let {
            userViewModel.saveIapSubs(it)
            onRefreshed()
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                userViewModel.reloadAccount()
                result.data?.let(IntentsUtil::getAccountAction)?.let {
                    when (it) {
                        AccountAction.Refreshed -> {
                            onRefreshed()
                        }
                        else -> {}
                    }
                }
            }
            Activity.RESULT_CANCELED -> {}
        }
    }

    if (account == null) {
        showSnackBar("Not logged in")
        return
    }

    if (showDialog) {
        CancelStripeDialog(
            onConfirm = {
                memberState.cancelStripe(account)
                setShowDialog(false)
            },
            onDismiss = {
                setShowDialog(false)
            }
        )
    }

    SwipeRefresh(
        state = rememberSwipeRefreshState(
            isRefreshing = memberState.refreshing
        ),
        onRefresh = {
            memberState.refresh(account)
        },
        modifier = modifier,
    ) {
        MemberScreen(
            member = account.membership
        ) {
            when (it) {
                SubsOptionRow.GoToPaywall -> {
                    SubsActivity.launch(
                        launcher = launcher,
                        context = context
                    )
                }
                SubsOptionRow.CancelStripe -> {
                    setShowDialog(true)
                }
                SubsOptionRow.ReactivateStripe -> {
                    memberState.reactivateStripe(account)
                }
            }
        }
    }
}

