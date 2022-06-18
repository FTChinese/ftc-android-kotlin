package com.ft.ftchinese.ui.account.delete

import android.content.res.Resources
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.request.AccountDropped
import com.ft.ftchinese.model.request.EmailPasswordParams
import com.ft.ftchinese.repository.AccountRepo
import com.ft.ftchinese.ui.util.ConnectionState
import com.ft.ftchinese.ui.util.connectivityState
import com.ft.ftchinese.ui.components.BaseState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class DeleteAccountState(
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
    resources: Resources,
    connState: State<ConnectionState>,
) : BaseState(scaffoldState, scope, resources, connState) {

    var dropped by mutableStateOf<AccountDropped?>(null)
        private set

    fun resetDropped() {
        dropped = null
    }

    fun drop(ftcId: String, params: EmailPasswordParams) {
        if (!ensureConnected()) {
            return
        }

        progress.value = false
        scope.launch {
            val result = AccountRepo.asyncDeleteAccount(
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
                    dropped = result.data
                }
            }
        }
    }
}

@Composable
fun rememberDeleteAccountState(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    scope: CoroutineScope = rememberCoroutineScope(),
    resources: Resources = LocalContext.current.resources,
    connState: State<ConnectionState> = connectivityState()
) = remember(scaffoldState, resources, connState) {
    DeleteAccountState(
        scaffoldState = scaffoldState,
        scope = scope,
        resources = resources,
        connState = connState
    )
}
