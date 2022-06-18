package com.ft.ftchinese.ui.account.overview

import android.content.res.Resources
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.repository.AccountRepo
import com.ft.ftchinese.ui.util.ConnectionState
import com.ft.ftchinese.ui.util.connectivityState
import com.ft.ftchinese.ui.components.BaseState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class FtcAccountState(
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
    resources: Resources,
    connState: State<ConnectionState>,
) : BaseState(scaffoldState, scope, resources, connState) {
    var refreshing by mutableStateOf(false)
        private set

    var alertDelete by mutableStateOf(false)
        private set

    var alertMobileEmail by mutableStateOf(false)
        private set

    var refreshed by mutableStateOf<Account?>(null)

    fun showDeleteAlert(show: Boolean) {
        alertDelete = show
    }

    fun showMobileAlert(show: Boolean) {
        alertMobileEmail = show
    }

    fun refresh(account: Account) {
        if (!ensureConnected()) {
            return
        }

        refreshing = true

        scope.launch {
            when (val result = AccountRepo.asyncRefresh(account)) {
                is FetchResult.LocalizedError -> {
                    showSnackBar(result.msgId)
                }
                is FetchResult.TextError -> {
                    showSnackBar(result.text)
                }
                is FetchResult.Success -> {
                    refreshed = result.data
                }
            }
            showRefreshed()
            refreshing = false
        }
    }
}

@Composable
fun rememberFtcAccountState(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    scope: CoroutineScope = rememberCoroutineScope(),
    resources: Resources = LocalContext.current.resources,
    connState: State<ConnectionState> = connectivityState()
) = remember(scaffoldState, resources) {
    FtcAccountState(
        scaffoldState = scaffoldState,
        scope = scope,
        resources = resources,
        connState = connState
    )
}
