package com.ft.ftchinese.ui.account

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.ui.stripewallet.StripeWalletActivity
import com.ft.ftchinese.ui.wxinfo.WxInfoActivity
import com.ft.ftchinese.viewmodel.UserViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@Composable
fun FtcAccountActivityScreen(
    userViewModel: UserViewModel,
    scaffold: ScaffoldState,
    onNavigateTo: (AccountAppScreen) -> Unit,
) {
    val context = LocalContext.current

    val uiState = rememberFtcAccountState(
        scaffoldState = scaffold
    )

    val accountState = userViewModel.accountLiveData.observeAsState()

    val account = accountState.value
    if (account == null) {
        uiState.showSnackBar("Not logged in")
        return
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

    if (uiState.alertDelete) {
        AlertDeleteAccount(
            onDismiss = {
                uiState.showDeleteAlert(false)
            },
            onConfirm = {
                uiState.showDeleteAlert(false)
                launchUpdateActivity(
                    launcher = launcher,
                    context = context,
                    rowId = AccountRowId.DELETE,
                )
            }
        )
    }

    if (uiState.alertMobileEmail) {
        MobileOnlyNotUpdatable(
            onDismiss = {
                uiState.showMobileAlert(false)
            }
        )
    }

    SwipeRefresh(
        state = rememberSwipeRefreshState(
            isRefreshing = uiState.refreshing
        ),
        onRefresh = {
            uiState.refresh(account)
        },
    ) {

        FtcAccountScreen(
            rows = buildAccountRows(
                context,
                account
            ),
            onClickRow = { rowId ->
                when (rowId) {
                    AccountRowId.DELETE -> {
                        uiState.showDeleteAlert(true)
                    }
                    AccountRowId.STRIPE -> {
                        StripeWalletActivity.start(context)
                    }
                    AccountRowId.WECHAT -> {
                        launchWxInfoActivity(
                            launcher = launcher,
                            context = context
                        )
                    }
                    AccountRowId.MOBILE -> {
                        if (account.isMobileEmail) {
                            uiState.showMobileAlert(true)
                        } else {
                            launchUpdateActivity(
                                launcher = launcher,
                                context = context,
                                rowId = rowId,
                            )
                        }
                    }
                    AccountRowId.EMAIL -> {
                        onNavigateTo(AccountAppScreen.Email)
                    }
                    AccountRowId.USER_NAME -> {
                        onNavigateTo(AccountAppScreen.UserName)
                    }
                    else -> {
                        launchUpdateActivity(
                            launcher = launcher,
                            context = context,
                            rowId = rowId,
                        )
                    }
                }
            }
        )
    }
}

private fun launchUpdateActivity(
    launcher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    context: Context,
    rowId: AccountRowId,
) {
    launcher.launch(
        UpdateActivity.intent(context, rowId)
    )
}

private fun launchWxInfoActivity(
    launcher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    context: Context,
) {
    launcher.launch(
        WxInfoActivity.newIntent(context)
    )
}
