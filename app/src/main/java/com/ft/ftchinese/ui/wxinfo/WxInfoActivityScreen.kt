package com.ft.ftchinese.ui.wxinfo

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.model.reader.WxOAuth
import com.ft.ftchinese.model.reader.WxOAuthIntent
import com.ft.ftchinese.ui.wxlink.LinkFtcActivity
import com.ft.ftchinese.ui.wxlink.UnlinkActivity
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI

@Composable
fun WxInfoActivityScreen(
    wxApi: IWXAPI,
    modifier: Modifier = Modifier,
    wxInfoViewModel: WxInfoViewModel = viewModel(),
    showSnackBar: (String) -> Unit,
) {
    val context = LocalContext.current
    val refreshing by wxInfoViewModel.refreshingLiveData.observeAsState(false)
    val reAuth by wxInfoViewModel.reAuthLiveData.observeAsState(false)
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                wxInfoViewModel.reloadAccount()
            }
            Activity.RESULT_CANCELED -> {}
        }
    }

    val account = remember {
        wxInfoViewModel.account
    }

    if (account == null) {
        showSnackBar("Not logged in")
        return
    }

    if (reAuth) {
        AlertWxLoginExpired(
            onDismiss = {
                wxInfoViewModel.clearReAuth()
            },
            onConfirm = {
                launchWxOAuth(wxApi)
                wxInfoViewModel.clearReAuth()
            }
        )
    }

    if (account.isEmailOnly) {
        AlertEmailLinkWx(
            onLinkWx = {
                launchWxOAuth(wxApi)
            }
        )
    } else {
        SwipeRefresh(
            state = rememberSwipeRefreshState(
                isRefreshing = refreshing
            ),
            onRefresh = {
                wxInfoViewModel.refreshWxInfo(account)
            },
            modifier = modifier,
        ) {
            WxInfoScreen(
                wechat = account.wechat,
                isLinked = account.isLinked,
                onLinkEmail = {
                    launchLinkEmailActivity(
                        launcher = launcher,
                        context = context
                    )
                },
                onUnlinkEmail = {
                    launchUnlinkEmailActivity(
                        launcher = launcher,
                        context = context
                    )
                }
            )
        }
    }
}

private fun launchWxOAuth(
    wxApi: IWXAPI
) {
    val stateCode = WxOAuth.generateStateCode(WxOAuthIntent.LINK)

    val req = SendAuth.Req()
    req.scope = WxOAuth.SCOPE
    req.state = stateCode

    wxApi.sendReq(req)
}

private fun launchLinkEmailActivity(
    launcher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    context: Context,
) {
    launcher.launch(
        LinkFtcActivity.intent(context)
    )
}

private fun launchUnlinkEmailActivity(
    launcher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    context: Context,
) {
    launcher.launch(
        UnlinkActivity.newIntent(context)
    )
}
