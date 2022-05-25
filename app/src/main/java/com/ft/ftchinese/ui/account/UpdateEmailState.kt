package com.ft.ftchinese.ui.account

import android.content.res.Resources
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.reader.BaseAccount
import com.ft.ftchinese.repository.AccountRepo
import com.ft.ftchinese.ui.base.ConnectionState
import com.ft.ftchinese.ui.base.connectivityState
import com.ft.ftchinese.ui.components.BaseState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class UpdateEmailState(
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
    resources: Resources,
    private val connState: State<ConnectionState>,
) : BaseState(scaffoldState, scope, resources) {

    val updated = mutableStateOf<BaseAccount?>(null)

    val isConnected: Boolean
        get() = connState.value == ConnectionState.Available

    fun ensureConnected(): Boolean {
        if (connState.value != ConnectionState.Available) {
            showNotConnected()
            return false
        }

        return true
    }

    fun requestVrfLetter(ftcId: String) {
        if (!ensureConnected()) {
            return
        }

        progress.value = true

        scope.launch {
            val doneResult = AccountRepo.asyncRequestVerification(ftcId)
            progress.value = false
            when (doneResult) {
                is FetchResult.LocalizedError -> {
                    showSnackBar(doneResult.msgId)
                }
                is FetchResult.TextError -> {
                    showSnackBar(doneResult.text)
                }
                is FetchResult.Success -> {
                    if (doneResult.data) {
                        showSnackBar(R.string.refresh_success)
                    } else {
                        showSnackBar(R.string.loading_failed)
                    }
                }
            }
        }
    }

    fun updateEmail(ftcId: String, email: String) {
        if (!ensureConnected()) {
            return
        }

        scope.launch {
            val result = AccountRepo.asyncUpdateEmail(
                ftcId = ftcId,
                email = email,
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
                    updated.value = result.data
                }
            }
        }
    }
}

@Composable
fun rememberUpdateEmailState(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    scope: CoroutineScope = rememberCoroutineScope(),
    resources: Resources = LocalContext.current.resources,
    connState: State<ConnectionState> = connectivityState()
) = remember {
    UpdateEmailState(
        scaffoldState = scaffoldState,
        scope = scope,
        resources = resources,
        connState = connState
    )
}
