package com.ft.ftchinese.wxapi.oauth

import android.content.res.Resources
import android.util.Log
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.WxOAuth
import com.ft.ftchinese.model.reader.WxOAuthKind
import com.ft.ftchinese.model.reader.WxSession
import com.ft.ftchinese.model.request.WxAuthParams
import com.ft.ftchinese.repository.AccountRepo
import com.ft.ftchinese.repository.AuthClient
import com.ft.ftchinese.ui.util.ConnectionState
import com.ft.ftchinese.ui.util.connectivityState
import com.ft.ftchinese.ui.components.BaseState
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.modelmsg.SendAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

sealed class AuthStatus {
    object Loading : AuthStatus()
    data class Failed(val message: String) : AuthStatus()
    data class LoginSuccess(val account: Account) : AuthStatus()
    data class LinkLoaded(val account: Account) : AuthStatus()
    object NotConnected : AuthStatus()
}

private const val TAG = "WxOAuthState"

class WxOAuthState(
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
    resources: Resources,
    connState: State<ConnectionState>,
) : BaseState(scaffoldState, scope, resources, connState) {

    var authStatus by mutableStateOf<AuthStatus>(AuthStatus.Loading)
        private set

    var wxSession by mutableStateOf<WxSession?>(null)
        private set

    private val codeState = mutableStateOf("")

    fun handleWxResp(resp: BaseResp) {
        when (resp.errCode) {
            BaseResp.ErrCode.ERR_OK -> {
                if (resp !is SendAuth.Resp) {
                    authStatus = AuthStatus.Failed(resp.errStr)
                    return
                }
                // 第一步：请求CODE
                // 用户点击授权后，微信客户端会被拉起，
                // 跳转至授权界面，用户在该界面点击允许或取消，
                // SDK通过SendAuth的Resp返回数据给调用方
                Log.i(TAG, "code: ${resp.code}, state: ${resp.state}, lang: ${resp.lang}, country: ${resp.country}")

                if (!WxOAuth.codeMatched(resp.state)) {
                    authStatus =
                        AuthStatus.Failed(resources.getString(R.string.oauth_state_mismatch))
                    return
                }

                Log.i(TAG, "Use oauth code to exchange for ftc login session...")
                getSession(code = resp.code)
            }
            BaseResp.ErrCode.ERR_USER_CANCEL -> {
                authStatus = AuthStatus.Failed(resources.getString(R.string.oauth_canceled))
            }
            BaseResp.ErrCode.ERR_AUTH_DENIED -> {
                authStatus = AuthStatus.Failed(resources.getString(R.string.oauth_denied))
            }
            BaseResp.ErrCode.ERR_SENT_FAILED,
            BaseResp.ErrCode.ERR_UNSUPPORT,
            BaseResp.ErrCode.ERR_BAN -> {
                authStatus = AuthStatus.Failed(resp.errStr ?: "Wechat SDK error")
            }
            else -> {
                authStatus = AuthStatus.Failed("Unknown errors occurred")
            }
        }
    }

    fun retry() {
        if (codeState.value.isNotBlank()) {
            authStatus = AuthStatus.Loading

            getSession(codeState.value)
        }
    }

    private fun getSession(code: String) {
        if (!isConnected) {
            authStatus = AuthStatus.NotConnected
            codeState.value = code
            return
        }

        val params = WxAuthParams(
            code = code,
        )

        scope.launch {
            val result = AuthClient.asyncWxLogin(params)

            when (result) {
                is FetchResult.LocalizedError -> {
                    authStatus = AuthStatus.Failed(resources.getString(result.msgId))
                }
                is FetchResult.TextError -> {
                    authStatus = AuthStatus.Failed(result.text)
                }
                is FetchResult.Success -> {
                    wxSession = result.data
                    loadWxAccount(result.data)
                }
            }
        }
    }

    private suspend fun loadWxAccount(session: WxSession) {
        val result = AccountRepo.asyncLoadWxAccount(session.unionId)

        when (result) {
            is FetchResult.LocalizedError -> {
                authStatus = AuthStatus.Failed(resources.getString(result.msgId))
            }
            is FetchResult.TextError -> {
                authStatus = AuthStatus.Failed(result.text)
            }
            is FetchResult.Success -> {
                when (WxOAuth.getLastIntent()) {
                    WxOAuthKind.LINK -> {
                        authStatus = AuthStatus.LinkLoaded(result.data)
                    }
                    else -> {
                        authStatus = AuthStatus.LoginSuccess(result.data)
                    }
                }
            }
        }
    }
}

@Composable
fun rememberWxOAuthState(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    scope: CoroutineScope = rememberCoroutineScope(),
    resources: Resources = LocalContext.current.resources,
    connState: State<ConnectionState> = connectivityState()
) = remember(scaffoldState, resources, connState) {
    WxOAuthState(
        scaffoldState = scaffoldState,
        scope = scope,
        resources = resources,
        connState = connState,
    )
}
