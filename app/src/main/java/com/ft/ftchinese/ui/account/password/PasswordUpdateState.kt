package com.ft.ftchinese.ui.account.password

import android.content.res.Resources
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.request.PasswordUpdateParams
import com.ft.ftchinese.model.request.PwUpdateResult
import com.ft.ftchinese.repository.AccountRepo
import com.ft.ftchinese.ui.util.ConnectionState
import com.ft.ftchinese.ui.util.connectivityState
import com.ft.ftchinese.ui.components.BaseState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class PasswordUpdateState(
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
    resources: Resources,
    connState: State<ConnectionState>,
) : BaseState(scaffoldState, scope, resources, connState) {

    var forgotPassword by mutableStateOf(false)
        private set

    fun closeForgotPassword() {
        forgotPassword = false
    }

    fun changePassword(ftcId: String, params: PasswordUpdateParams) {
        if (!ensureConnected()) {
            return
        }

        progress.value = true
        scope.launch {
            val result = AccountRepo.asyncUpdatePassword(
                ftcId = ftcId,
                params = params
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
                    when (result.data) {
                        PwUpdateResult.Done -> {
                            showSnackBar(R.string.prompt_saved)
                        }
                        PwUpdateResult.Mismatched -> {
                            forgotPassword = true
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun rememberPasswordState(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    scope: CoroutineScope = rememberCoroutineScope(),
    resources: Resources = LocalContext.current.resources,
    connState: State<ConnectionState> = connectivityState()
) = remember(scaffoldState, resources, connState) {
    PasswordUpdateState(
        scaffoldState = scaffoldState,
        scope = scope,
        resources = resources,
        connState = connState
    )
}
