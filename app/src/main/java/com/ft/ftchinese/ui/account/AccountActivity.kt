package com.ft.ftchinese.ui.account

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.ShowToast
import com.ft.ftchinese.ui.components.Toolbar
import com.ft.ftchinese.ui.stripewallet.StripeWalletActivity
import com.ft.ftchinese.ui.theme.OTheme
import com.ft.ftchinese.ui.wxinfo.WxInfoActivity
import com.ft.ftchinese.ui.wxinfo.WxInfoActivityScreen
import com.ft.ftchinese.ui.wxinfo.WxInfoViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import org.jetbrains.anko.toast

/**
 * Show user's account details.
 * Show different fragments based on whether FTC account is bound to wechat account.
 * If user logged in with email account, show FtcAccountFragment;
 * If user logged in with wechat account and it is not bound to an FTC account, show WxAccountFragment;
 * If user logged in with wechat account and it is bound to an FTC account, show FtcAccountFragment.
 */
class AccountActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OTheme {
                val scaffoldState = rememberScaffoldState()

                Scaffold(
                    topBar = {
                        Toolbar(
                            heading = stringResource(id = R.string.title_account),
                            onBack = {
                                setResult(RESULT_OK)
                                finish()
                            }
                        )
                    },
                    scaffoldState = scaffoldState
                ) { innerPadding ->
                    AccountActivityScreen(
                        modifier = Modifier.padding(innerPadding),
                        showSnackBar = {
                            toast(it)
                        }
                    )
                }
            }
        }
    }

    companion object {

        @JvmStatic
        fun start(context: Context) {
            val intent = Intent(context, AccountActivity::class.java)
            context.startActivity(intent)
        }
    }
}

@Composable
fun AccountActivityScreen(
    modifier: Modifier = Modifier,
    accountViewModel: WxInfoViewModel = viewModel(),
    showSnackBar: (String) -> Unit
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
                    rowId = AccountRowType.DELETE,
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
                        AccountRowType.DELETE -> {
                            setShowDelete(true)
                        }
                        AccountRowType.STRIPE -> {
                            StripeWalletActivity.start(context)
                        }
                        AccountRowType.WECHAT -> {
                            launchWxInfoActivity(
                                launcher = launcher,
                                context = context
                            )
                        }
                        AccountRowType.MOBILE -> {
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
    rowId: AccountRowType,
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
