package com.ft.ftchinese.ui.account.overview

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ScaffoldState
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.ui.account.AccountAppScreen
import com.ft.ftchinese.ui.account.AccountRowId
import com.ft.ftchinese.ui.account.buildAccountRows
import com.ft.ftchinese.viewmodel.UserViewModel

private const val TAG = "FtcAccount"

@OptIn(ExperimentalMaterialApi::class)
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

    LaunchedEffect(key1 = uiState.refreshed) {
        uiState.refreshed?.let {
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

    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.refreshing,
        onRefresh = {
            uiState.refresh(account)
        },
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
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
                        onNavigateTo(AccountAppScreen.Stripe)
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
        PullRefreshIndicator(
            refreshing = uiState.refreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
