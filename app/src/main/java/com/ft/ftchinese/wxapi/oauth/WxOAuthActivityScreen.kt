package com.ft.ftchinese.wxapi.oauth

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LiveData
import com.ft.ftchinese.model.enums.LoginMethod
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.util.toast
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.wxlink.merge.MergerStore
import com.ft.ftchinese.ui.wxlink.merge.WxEmailMerger
import com.tencent.mm.opensdk.modelbase.BaseResp

@Composable
fun WxOAuthActivityScreen(
    wxRespLiveData: LiveData<BaseResp?>,
    onFinish: () -> Unit,
    onLink: () -> Unit
) {

    val context = LocalContext.current

    val sessionStore = remember {
        SessionManager.getInstance(context)
    }

    val oauthState = rememberWxOAuthState()

    val wxRespState = wxRespLiveData.observeAsState()

    LaunchedEffect(key1 = wxRespState.value) {
        wxRespState.value?.let {
            oauthState.handleWxResp(it)
        }
    }

    LaunchedEffect(key1 = oauthState.wxSession) {
        oauthState.wxSession?.let {
            sessionStore.saveWxSession(it)
        }
    }

    LaunchedEffect(key1 = oauthState.authStatus) {
        when (val status = oauthState.authStatus) {
            is AuthStatus.LinkLoaded -> {
                val current = sessionStore.loadAccount()
                if (current == null) {
                    context.toast("Cannot perform link wechat: not logged in")
                    return@LaunchedEffect
                }

                MergerStore.setMerger(
                    WxEmailMerger(
                        ftc = current,
                        wx = status.account,
                        loginMethod = current.loginMethod ?: LoginMethod.EMAIL
                    )
                )
                onLink()
            }
            is AuthStatus.LoginSuccess -> {
                sessionStore.saveAccount(status.account)
            }
            else -> {}
        }
    }

    ProgressLayout(
        loading = oauthState.authStatus is AuthStatus.Loading,
        modifier = Modifier.fillMaxSize()
    ) {
        WxOAuthAScreen(
            status = oauthState.authStatus,
            onFinish = onFinish,
            onLink = {
                val current = sessionStore.loadAccount()
                if (current == null) {
                    context.toast("Cannot perform link wechat: not logged in")
                    return@WxOAuthAScreen
                }

                MergerStore.setMerger(
                    WxEmailMerger(
                        ftc = current,
                        wx = it,
                        loginMethod = current.loginMethod ?: LoginMethod.EMAIL
                    )
                )
                onLink()
            },
            onRetry = oauthState::retry
        )
    }
}
