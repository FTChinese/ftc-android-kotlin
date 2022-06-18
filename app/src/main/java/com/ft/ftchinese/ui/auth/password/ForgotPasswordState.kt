package com.ft.ftchinese.ui.auth.password

import android.content.res.Resources
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.reader.PwResetBearer
import com.ft.ftchinese.model.request.PasswordResetLetterParams
import com.ft.ftchinese.model.request.PasswordResetVerifier
import com.ft.ftchinese.repository.AuthClient
import com.ft.ftchinese.ui.util.ConnectionState
import com.ft.ftchinese.ui.util.connectivityState
import com.ft.ftchinese.ui.components.BaseState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ForgotPasswordState(
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
    resources: Resources,
    connState: State<ConnectionState>,
) : BaseState(scaffoldState, scope, resources, connState) {

    var letterSent by mutableStateOf(false)
        private set

    var verified by mutableStateOf<PwResetBearer?>(null)
        private set

    fun requestCode(params: PasswordResetLetterParams) {
        if (!ensureConnected()) {
            return
        }

        letterSent = false
        progress.value = true
        scope.launch {
            val result = AuthClient.asyncPasswordResetLetter(
                params
            )

            progress.value = false
            when (result) {
                is FetchResult.LocalizedError -> {
                    showSnackBar(result.msgId)
                }
                is FetchResult.TextError -> {
                    showSnackBar(result.text)
                }
                is FetchResult.Success -> {
                    if (result.data) {
                        letterSent = true
                    } else {
                        showSnackBar("邮件发送失败，未知错误！")
                    }
                }
            }
        }
    }

    fun verifyCode(params: PasswordResetVerifier) {
        verified = null

        if (!ensureConnected()) {
            return
        }

        progress.value = true
        scope.launch {
            val result = AuthClient.asyncVerifyPwResetCode(
                params
            )

            progress.value = false

            when (result) {
                is FetchResult.LocalizedError -> {
                    showSnackBar(result.msgId)
                }
                is FetchResult.TextError -> {
                    showSnackBar(result.text)
                }
                is FetchResult.Success -> {
                    verified = result.data
                }
            }
        }
    }
}

@Composable
fun rememberForgotState(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    scope: CoroutineScope = rememberCoroutineScope(),
    resources: Resources = LocalContext.current.resources,
    connState: State<ConnectionState> = connectivityState()
) = remember(scaffoldState, resources, connState) {
    ForgotPasswordState(
        scaffoldState = scaffoldState,
        scope = scope,
        resources = resources,
        connState = connState
    )
}


