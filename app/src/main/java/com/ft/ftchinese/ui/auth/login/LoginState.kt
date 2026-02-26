package com.ft.ftchinese.ui.auth.login

import android.content.res.Resources
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.request.Credentials
import com.ft.ftchinese.repository.AuthClient
import com.ft.ftchinese.ui.util.ConnectionState
import com.ft.ftchinese.ui.util.connectivityState
import com.ft.ftchinese.ui.components.BaseState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * LoginState could in used in 2 cases:
 * - Direct login with email account. In such case you should save the account returned from API;
 * -A wechat-only user wants to link to an existing email account. In such case you should
 *  first verify the email account and DO NOT save the returned account; otherwise you are risking
 *  overriding current account save on device. Use the returned account to show a screen to perform
 *  link.
 */
open class LoginState(
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
    resources: Resources,
    connState: State<ConnectionState>,
) : BaseState(scaffoldState, scope, resources, connState) {
    var emailAccount by mutableStateOf<Account?>(null)

    fun authenticate(credentials: Credentials) {
        if (!ensureConnected()) {
            return
        }

        progress.value = true
        scope.launch {
            val result = AuthClient.asyncEmailLogin(credentials)
            progress.value = false
            when (result) {
                is FetchResult.LocalizedError -> {
                    showSnackBar(result.msgId)
                }
                is FetchResult.TextError -> {
                    showSnackBar(result.text)
                }
                is FetchResult.Success -> {
                    emailAccount = result.data
                }
            }
        }
    }

    fun authenticateSso(token: String) {
        if (token.isBlank() || !ensureConnected()) {
            return
        }

        progress.value = true
        scope.launch {
            val result = AuthClient.asyncSsoLogin(token)
            progress.value = false
            when (result) {
                is FetchResult.LocalizedError -> {
                    showSnackBar(result.msgId)
                }
                is FetchResult.TextError -> {
                    showSnackBar(result.text)
                }
                is FetchResult.Success -> {
                    emailAccount = result.data
                }
            }
        }
    }
}

@Composable
fun rememberLoginState(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    scope: CoroutineScope = rememberCoroutineScope(),
    resources: Resources = LocalContext.current.resources,
    connState: State<ConnectionState> = connectivityState()
) = remember(scaffoldState, resources, connState) {
    LoginState(
        scaffoldState = scaffoldState,
        scope = scope,
        resources = resources,
        connState = connState
    )
}
