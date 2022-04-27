package com.ft.ftchinese.ui.member

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.ui.SubsActivity
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@Composable
fun MemberActivityScreen(
    memberViewModel: MembershipViewModel,
    showSnackBar: (String) -> Unit,
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current

    val isRefreshing by memberViewModel.refreshingLiveData.observeAsState(false)
    val progress by memberViewModel.progressLiveData.observeAsState(false)
    val account = remember {
        memberViewModel.account
    }
    val (showDialog, setShowDialog) = remember {
        mutableStateOf(false)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                memberViewModel.reloadAccount()
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
                memberViewModel.cancelStripe(account)
                setShowDialog(false)
            },
            onDismiss = {
                setShowDialog(false)
            }
        )
    }

    SwipeRefresh(
        state = rememberSwipeRefreshState(
            isRefreshing = isRefreshing
        ),
        onRefresh = {
            memberViewModel.refresh()
        },
        modifier = modifier,
    ) {
        MemberScreen(
            member = account.membership,
            loading = progress,
            onSubsOption = {
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
                        memberViewModel.reactivateStripe(account)
                    }
                }
            }
        )
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
