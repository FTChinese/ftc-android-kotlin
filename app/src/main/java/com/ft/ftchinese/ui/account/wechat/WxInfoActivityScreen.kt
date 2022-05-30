package com.ft.ftchinese.ui.account.wechat

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.wxinfo.AlertWxLoginExpired
import com.ft.ftchinese.ui.wxinfo.launchWxOAuth
import com.ft.ftchinese.ui.wxlink.launchWxLinkEmailActivity
import com.ft.ftchinese.viewmodel.UserViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.tencent.mm.opensdk.openapi.WXAPIFactory

@Composable
fun WxInfoActivityScreen(
    userViewModel: UserViewModel,
    scaffold: ScaffoldState,
    onUnlink: () -> Unit,
) {
    val context = LocalContext.current
    val accountState = userViewModel.accountLiveData.observeAsState()
    val account = accountState.value

    val uiState = rememberWxInfoState(
        scaffoldState = scaffold
    )

    val sessionStore = remember {
        SessionManager.getInstance(context)
    }

    val wxApi = remember {
        WXAPIFactory.createWXAPI(
            context,
            BuildConfig.WX_SUBS_APPID
        )
    }

    val (reAuth, setReAuth) = remember {
        mutableStateOf(false)
    }

    LaunchedEffect(key1 = uiState.refreshed) {
        uiState.refreshed?.let {
            userViewModel.saveAccount(it)
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
        uiState.showSnackBar("Not logged in")
        return
    }

    if (reAuth) {
        AlertWxLoginExpired(
            onDismiss = {
                setReAuth(false)
            },
            onConfirm = {
                launchWxOAuth(wxApi)
                setReAuth(false)
            }
        )
    }

    if (account.isEmailOnly) {
        EmailLinkWxScreen(
            onLinkWx = {
                launchWxOAuth(wxApi)
            }
        )
    } else {
        SwipeRefresh(
            state = rememberSwipeRefreshState(
                isRefreshing = uiState.refreshing
            ),
            onRefresh = {
                val wxSession = sessionStore.loadWxSession()
                if (wxSession == null) {
                    setReAuth(true)
                    return@SwipeRefresh
                }
                uiState.refresh(account, wxSession)
            },
        ) {
            WxInfoScreen(
                wechat = account.wechat,
                isLinked = account.isLinked,
                onLinkEmail = {
                    launchWxLinkEmailActivity(
                        launcher = launcher,
                        context = context
                    )
                },
                onUnlinkEmail = onUnlink
            )
        }
    }
}

