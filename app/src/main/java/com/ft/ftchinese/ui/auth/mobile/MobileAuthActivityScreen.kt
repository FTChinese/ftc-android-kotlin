package com.ft.ftchinese.ui.auth.mobile

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.WxOAuth
import com.ft.ftchinese.model.reader.WxOAuthKind
import com.ft.ftchinese.model.request.MobileAuthParams
import com.ft.ftchinese.model.request.MobileSignUpParams
import com.ft.ftchinese.model.request.SMSCodeParams
import com.ft.ftchinese.store.TokenManager
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.components.rememberTimerState
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory

@Composable
fun MobileAuthActivityScreen(
    scaffoldState: ScaffoldState,
    onLinkEmail: (String) -> Unit, // Pass mobile number to next screen.
    onEmailLogin: () -> Unit,
    onFinish: () -> Unit,
    onSuccess: (Account) -> Unit,
) {
    val context = LocalContext.current

    val tokenStore = remember {
        TokenManager.getInstance(context)
    }

    val wxApi = remember {
        WXAPIFactory.createWXAPI(
            context,
            BuildConfig.WX_SUBS_APPID
        )
    }

    val authState = rememberMobileAuthState(
        scaffoldState = scaffoldState
    )

    val timerState = rememberTimerState()

    LaunchedEffect(key1 = authState.codeSent) {
        if (authState.codeSent && !timerState.isRunning) {
            timerState.start()
        }
    }

    LaunchedEffect(key1 = authState.accountLoaded) {
        authState.accountLoaded?.let {
            onSuccess(it)
        }
    }

    if (authState.mobileNotSet.isNotBlank()) {
        AlertMobileNotSet(
            onLinkEmail = {
                onLinkEmail(authState.mobileNotSet)
                authState.closeNotSetAlert()
            },
            onSignUp = {
                authState.createAccount(
                    MobileSignUpParams(
                        mobile = authState.mobileNotSet,
                        deviceToken = tokenStore.getToken()
                    )
                )
            }
        )
    }

    ProgressLayout(
        loading = authState.progress.value
    ) {
        MobileAuthScreen(
            loading = authState.progress.value,
            timerState = timerState,
            onRequestCode = {
                authState.requestAuthCode(
                    SMSCodeParams(
                        mobile = it
                    )
                )
            },
            onSubmit = { formVal ->
                authState.verifySMSCode(
                    params = MobileAuthParams(
                        mobile = formVal.mobile,
                        code = formVal.code,
                        deviceToken = tokenStore.getToken()
                    )
                )
            },
            alternative = {
                LoginAlternatives(
                    onClickEmail = onEmailLogin,
                    onClickWechat = {
                        launchWxOAuth(wxApi)
                        onFinish()
                    }
                )
            }
        )
    }
}

private fun launchWxOAuth(
    wxApi: IWXAPI
) {
    val stateCode = WxOAuth.generateStateCode(WxOAuthKind.LOGIN)

    val req = SendAuth.Req()
    req.scope = WxOAuth.SCOPE
    req.state = stateCode

    wxApi.sendReq(req)
}
