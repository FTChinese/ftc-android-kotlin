package com.ft.ftchinese.ui.subs.member

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.ui.SubsActivity
import com.ft.ftchinese.viewmodel.UserViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@Composable
fun MemberActivityScreen(
    modifier: Modifier = Modifier,
    userViewModel: UserViewModel = viewModel(),
    scaffoldState: ScaffoldState,
    showSnackBar: (String) -> Unit,
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
        }
    }

    LaunchedEffect(key1 = memberState.stripeSubsUpdated) {
        memberState.stripeSubsUpdated?.let {
            userViewModel.saveStripeSubs(it)
        }
    }

    LaunchedEffect(key1 = memberState.iapSubsUpdated) {
        memberState.iapSubsUpdated?.let {
            userViewModel.saveIapSubs(it)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                userViewModel.reloadAccount()
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
                    launchPaywallActivity(
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

private fun launchPaywallActivity(
    launcher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    context: Context
) {
    launcher.launch(
        SubsActivity.intent(context)
    )
}
