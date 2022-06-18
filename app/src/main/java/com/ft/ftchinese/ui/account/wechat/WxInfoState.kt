package com.ft.ftchinese.ui.account.wechat

import android.content.res.Resources
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.WxSession
import com.ft.ftchinese.repository.AccountRepo
import com.ft.ftchinese.ui.util.ConnectionState
import com.ft.ftchinese.ui.util.connectivityState
import com.ft.ftchinese.ui.components.BaseState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class WxInfoState(
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
    resources: Resources,
    connState: State<ConnectionState>,
) : BaseState(scaffoldState, scope, resources, connState)  {
    var refreshing by mutableStateOf(false)
        private set

    var refreshed by mutableStateOf<Account?>(null)
        private set

    fun refresh(a: Account, sess: WxSession) {
        if (!ensureConnected()) {
            return
        }

        refreshing = true
        scope.launch {
            val done = AccountRepo.asyncRefreshWxInfo(sess)
            refreshing = false

            when (done) {
                is FetchResult.LocalizedError -> {
                    refreshing = false
                    showSnackBar(done.msgId)
                }
                is FetchResult.TextError -> {
                    refreshing = false
                    showSnackBar(done.text)
                }
                is FetchResult.Success -> {
                    if (!done.data) {
                        showSnackBar(R.string.loading_failed)
                        refreshing = false
                        return@launch
                    }

                    loadAccount(a)
                }
            }
        }
    }

    suspend fun loadAccount(account: Account) {
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

@Composable
fun rememberWxInfoState(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    scope: CoroutineScope = rememberCoroutineScope(),
    resources: Resources = LocalContext.current.resources,
    connState: State<ConnectionState> = connectivityState()
) = remember(scaffoldState, resources) {
    WxInfoState(
        scaffoldState = scaffoldState,
        scope = scope,
        resources = resources,
        connState = connState
    )
}
