package com.ft.ftchinese.ui.auth.mobile

import android.content.res.Resources
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.request.MobileAuthParams
import com.ft.ftchinese.model.request.MobileSignUpParams
import com.ft.ftchinese.model.request.SMSCodeParams
import com.ft.ftchinese.repository.AccountRepo
import com.ft.ftchinese.repository.AuthClient
import com.ft.ftchinese.ui.util.ConnectionState
import com.ft.ftchinese.ui.util.connectivityState
import com.ft.ftchinese.ui.components.BaseState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MobileAuthState(
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
    resources: Resources,
    connState: State<ConnectionState>,
) : BaseState(scaffoldState, scope, resources, connState) {

    // After sms code is sent to user device, start counting down.
    var codeSent by mutableStateOf(false)
        private set

    // If a mobile number is not linked to any email account,
    // set this value to trigger an alert dialog, which
    // will pass the mobile number to either create a new account
    // or link to an existing email account.
    var mobileNotSet by mutableStateOf("")
        private set

    // The account after login success with mobile, or new account
    // created.
    var accountLoaded by mutableStateOf<Account?>(null)
        private set

    fun closeNotSetAlert() {
        mobileNotSet = ""
    }

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
                    val id = result.data.id
                    if (id == null) {
                        mobileNotSet = params.mobile
                        return@launch
                    }

                    // continue loading data...
                    loadAccount(id)
                }
            }
        }
    }

    private suspend fun loadAccount(ftcId: String) {
        val result = AccountRepo.asyncLoadFtcAccount(ftcId)
        progress.value = false
        when (result) {
            is FetchResult.LocalizedError -> {
                showSnackBar(result.msgId)
            }
            is FetchResult.TextError -> {
                showSnackBar(result.text)
            }
            is FetchResult.Success -> {
                accountLoaded = result.data
            }
        }
    }

    fun createAccount(params: MobileSignUpParams) {
        if (!ensureConnected()) {
            return
        }

        progress.value = true
        scope.launch {
            val result = AuthClient.asyncMobileSignUp(params)
            progress.value = false
            when (result) {
                is FetchResult.LocalizedError -> {
                    showSnackBar(result.msgId)
                }
                is FetchResult.TextError -> {
                    showSnackBar(result.text)
                }
                is FetchResult.Success -> {
                    accountLoaded = result.data
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
