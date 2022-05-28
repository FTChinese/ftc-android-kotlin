package com.ft.ftchinese.ui.auth.mobile

import android.content.res.Resources
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.BaseAccount
import com.ft.ftchinese.model.request.MobileAuthParams
import com.ft.ftchinese.model.request.SMSCodeParams
import com.ft.ftchinese.repository.AuthClient
import com.ft.ftchinese.ui.base.ConnectionState
import com.ft.ftchinese.ui.base.connectivityState
import com.ft.ftchinese.ui.components.BaseState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MobileAuthState(
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
    resources: Resources,
    connState: State<ConnectionState>,
) : BaseState(scaffoldState, scope, resources, connState) {

    var codeSent by mutableStateOf(false)
        private set

    var mobileNotSet by mutableStateOf(false)
        private set

    var accountLoaded by mutableStateOf<Account?>(null)
        private set

    fun requestAuthCode(params: SMSCodeParams) {
        if (!ensureConnected()) {
            return
        }

        codeSent = false
        progress.value = true

        scope.launch {
            val result = AuthClient.asyncRequestSMSCode(params)

            progress.value = false
            when (result) {
                is FetchResult.LocalizedError -> {
                    showSnackBar(result.msgId)
                }
                is FetchResult.TextError -> {
                    showSnackBar(result.text)
                }
                is FetchResult.Success -> {
                    showSnackBar(R.string.sms_code_sent)
                    codeSent = result.data
                }
            }
        }
    }

    fun verifySMSCode(params: MobileAuthParams) {
        if (!ensureConnected()) {
            return
        }

        progress.value = true

        scope.launch {
            val result = AuthClient.asyncVerifySMSCode(params)

            progress.value = false
            when (result) {
                is FetchResult.LocalizedError -> {
                    showSnackBar(result.msgId)
                }
                is FetchResult.TextError -> {
                    showSnackBar(result.text)
                }
                is FetchResult.Success -> {
                    showSnackBar(R.string.sms_code_sent)
                    val id = result.data.id
                    if (id == null) {
                        mobileNotSet = true
                        return@launch
                    }

                    // continue loading data...
                }
            }
        }
    }
}

@Composable
fun rememberMobileAuthState(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    scope: CoroutineScope = rememberCoroutineScope(),
    resources: Resources = LocalContext.current.resources,
    connState: State<ConnectionState> = connectivityState()
) = remember(scaffoldState, resources, connState) {
    MobileAuthState(
        scaffoldState = scaffoldState,
        scope = scope,
        resources = resources,
        connState = connState
    )
}
