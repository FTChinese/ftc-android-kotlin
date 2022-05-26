package com.ft.ftchinese.ui.account

import android.util.Log
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.ui.stripewallet.StripeWalletActivity
import com.ft.ftchinese.viewmodel.UserViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

private const val TAG = "FtcAccount"

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

    LaunchedEffect(key1 = uiState.accountRefreshed.value) {
        uiState.accountRefreshed.value?.let {
            Log.i(TAG, "Save refreshed account")
            userViewModel.saveAccount(it)
        }
    }

    if (uiState.alertDelete) {
        AlertDeleteAccount(
            onDismiss = {
                uiState.showDeleteAlert(false)
            },
            onConfirm = {
                uiState.showDeleteAlert(false)
                onNavigateTo(AccountAppScreen.DeleteAccount)
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
                    AccountRowId.EMAIL -> {
                        onNavigateTo(AccountAppScreen.Email)
                    }
                    AccountRowId.USER_NAME -> {
                        onNavigateTo(AccountAppScreen.UserName)
                    }
                    AccountRowId.PASSWORD -> {
                        onNavigateTo(AccountAppScreen.Password)
                    }
                    AccountRowId.Address -> {
                        onNavigateTo(AccountAppScreen.Address)
                    }
                    AccountRowId.STRIPE -> {
                        StripeWalletActivity.start(context)
                    }
                    AccountRowId.WECHAT -> {
                        onNavigateTo(AccountAppScreen.Wechat)
                    }
                    AccountRowId.MOBILE -> {
                        if (account.isMobileEmail) {
                            uiState.showMobileAlert(true)
                        } else {
                            onNavigateTo(AccountAppScreen.Mobile)
                        }
                    }
                    AccountRowId.DELETE -> {
                        uiState.showDeleteAlert(true)
                    }
                }
            }
        )
    }
}

