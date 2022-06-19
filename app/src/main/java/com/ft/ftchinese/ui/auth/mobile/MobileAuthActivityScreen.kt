package com.ft.ftchinese.ui.auth.mobile

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.model.request.MobileAuthParams
import com.ft.ftchinese.model.request.MobileSignUpParams
import com.ft.ftchinese.model.request.SMSCodeParams
import com.ft.ftchinese.store.TokenManager
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.components.launchWxLogin
import com.ft.ftchinese.ui.components.rememberTimerState
import com.ft.ftchinese.ui.components.rememberWxApi
import com.ft.ftchinese.viewmodel.UserViewModel

@Composable
fun MobileAuthActivityScreen(
    userViewModel: UserViewModel,
    scaffoldState: ScaffoldState,
    onLinkEmail: (String) -> Unit, // Pass mobile number to next screen.
    onEmailLogin: () -> Unit,
    onFinish: () -> Unit,
    onSuccess: () -> Unit,
) {
    val context = LocalContext.current

    val tokenStore = remember {
        TokenManager.getInstance(context)
    }

    val wxApi = rememberWxApi()

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
            userViewModel.saveAccount(it)
            onSuccess()
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
        loading = authState.progress.value,
        modifier = Modifier.fillMaxSize()
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
                        launchWxLogin(wxApi)
                        onFinish()
                    }
                )
            }
        )
    }
}

