package com.ft.ftchinese.wxapi

import android.content.res.Resources
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.WxOAuth
import com.ft.ftchinese.model.reader.WxOAuthIntent
import com.ft.ftchinese.model.reader.WxSession
import com.ft.ftchinese.model.request.WxAuthParams
import com.ft.ftchinese.repository.AccountRepo
import com.ft.ftchinese.repository.AuthClient
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.ConnectionState
import com.ft.ftchinese.ui.base.connectivityState
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
    private val scope: CoroutineScope,
    private val resources: Resources,
    private val connState: State<ConnectionState>,
    private val sessionStore: SessionManager,
) {

    val authStatus = mutableStateOf<AuthStatus>(AuthStatus.Loading)
    private val codeState = mutableStateOf("")

    private val isConnected: Boolean
        get() = connState.value == ConnectionState.Available

    fun handleWxResp(resp: BaseResp) {
        when (resp.errCode) {
            BaseResp.ErrCode.ERR_OK -> {
                if (resp !is SendAuth.Resp) {
                    authStatus.value = AuthStatus.Failed(resp.errStr)
                    return
                }
                // 第一步：请求CODE
                // 用户点击授权后，微信客户端会被拉起，
                // 跳转至授权界面，用户在该界面点击允许或取消，
                // SDK通过SendAuth的Resp返回数据给调用方
                Log.i(TAG, "code: ${resp.code}, state: ${resp.state}, lang: ${resp.lang}, country: ${resp.country}")

                if (!WxOAuth.codeMatched(resp.state)) {
                    authStatus.value = AuthStatus.Failed(resources.getString(R.string.oauth_state_mismatch))
                    return
                }

                Log.i(TAG, "Use oauth code to exchange for ftc login session...")
                getSession(code = resp.code)
            }
            BaseResp.ErrCode.ERR_USER_CANCEL -> {
                authStatus.value = AuthStatus.Failed(resources.getString(R.string.oauth_canceled))
            }
            BaseResp.ErrCode.ERR_AUTH_DENIED -> {
                authStatus.value = AuthStatus.Failed(resources.getString(R.string.oauth_denied))
            }
            BaseResp.ErrCode.ERR_SENT_FAILED,
            BaseResp.ErrCode.ERR_UNSUPPORT,
            BaseResp.ErrCode.ERR_BAN -> {
                authStatus.value = AuthStatus.Failed(resp.errStr)
            }
            else -> {
                authStatus.value = AuthStatus.Failed("Unknown errors occurred")
            }
        }
    }

    fun retry() {
        if (codeState.value.isNotBlank()) {
            authStatus.value = AuthStatus.Loading

            getSession(codeState.value)
        }
    }

    private fun getSession(code: String) {
        if (!isConnected) {
            authStatus.value = AuthStatus.NotConnected
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
                    authStatus.value = AuthStatus.Failed(resources.getString(result.msgId))
                }
                is FetchResult.TextError -> {
                    authStatus.value = AuthStatus.Failed(result.text)
                }
                is FetchResult.Success -> {
                    sessionStore.saveWxSession(result.data)
                    loadWxAccount(result.data)
                }
            }
        }
    }

    private suspend fun loadWxAccount(session: WxSession) {
        val result = AccountRepo.asyncLoadWxAccount(session.unionId)

        when (result) {
            is FetchResult.LocalizedError -> {
                authStatus.value = AuthStatus.Failed(resources.getString(result.msgId))
            }
            is FetchResult.TextError -> {
                authStatus.value = AuthStatus.Failed(result.text)
            }
            is FetchResult.Success -> {
                when (WxOAuth.getLastIntent()) {
                    WxOAuthIntent.LINK -> {
                        authStatus.value = AuthStatus.LinkLoaded(result.data)
                    }
                    else -> {
                        sessionStore.saveAccount(result.data)
                        authStatus.value = AuthStatus.LoginSuccess(result.data)
                    }
                }
            }
        }
    }
}

@Composable
fun rememberWxOAuthState(
    scope: CoroutineScope = rememberCoroutineScope(),
    resources: Resources = LocalContext.current.resources,
    connState: State<ConnectionState> = connectivityState(),
    sessionStore: SessionManager,
) = remember(scope, resources, connState) {
    WxOAuthState(
        scope = scope,
        resources = resources,
        connState = connState,
        sessionStore = sessionStore
    )
}
