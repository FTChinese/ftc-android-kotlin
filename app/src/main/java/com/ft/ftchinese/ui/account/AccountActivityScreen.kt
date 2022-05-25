package com.ft.ftchinese.ui.account

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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.ui.components.ShowToast
import com.ft.ftchinese.ui.stripewallet.StripeWalletActivity
import com.ft.ftchinese.ui.wxinfo.WxInfoActivity
import com.ft.ftchinese.ui.wxinfo.WxInfoActivityScreen
import com.ft.ftchinese.ui.wxinfo.WxInfoViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.tencent.mm.opensdk.openapi.WXAPIFactory

@Composable
fun AccountActivityScreen(
    modifier: Modifier = Modifier,
    accountViewModel: WxInfoViewModel = viewModel(),
    showSnackBar: (String) -> Unit,
    onNavigateTo: (AccountAppScreen) -> Unit,
) {
    val context = LocalContext.current
    val refreshing by accountViewModel.refreshingLiveData.observeAsState(false)
    val messageState = accountViewModel.toastLiveData.observeAsState(null)
    val (showDelete, setShowDelete) = remember {
        mutableStateOf(false)
    }

    val (showMobileAlert, setShowMobileAlert) = remember {
        mutableStateOf(false)
    }
    val accountState = accountViewModel.accountLiveData.observeAsState()

    val account = accountState.value
    if (account == null) {
        showSnackBar("Not logged in")
        return
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                accountViewModel.reloadAccount()
            }
            Activity.RESULT_CANCELED -> {}
        }
    }

    if (showDelete) {
        AlertDeleteAccount(
            onDismiss = {
                setShowDelete(false)
            },
            onConfirm = {
                setShowDelete(false)
                launchUpdateActivity(
                    launcher = launcher,
                    context = context,
                    rowId = AccountRowId.DELETE,
                )
            }
        )
    }

    if (showMobileAlert) {
        MobileOnlyNotUpdatable(
            onDismiss = {
                setShowMobileAlert(false)
            }
        )
    }

    ShowToast(
        toast = messageState.value
    ) {
        accountViewModel.resetToast()
    }

    if (account.isWxOnly) {
        WxInfoActivityScreen(
            wxApi = WXAPIFactory.createWXAPI(
                context,
                BuildConfig.WX_SUBS_APPID
            ).apply {
                registerApp(BuildConfig.WX_SUBS_APPID)
            },
            modifier = modifier,
            wxInfoViewModel = accountViewModel,
            showSnackBar = showSnackBar,
            onUpdated = {
                accountViewModel.reloadAccount()
            }
        )
    } else {
        SwipeRefresh(
            state = rememberSwipeRefreshState(
                isRefreshing = refreshing
            ),
            onRefresh = {
                accountViewModel.refreshAccount()
            },
            modifier = modifier
        ) {

            FtcAccountScreen(
                rows = buildAccountRows(
                    context,
                    account
                ),
                onClickRow = { rowId ->
                    when (rowId) {
                        AccountRowId.DELETE -> {
                            setShowDelete(true)
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
                                setShowMobileAlert(true)
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
