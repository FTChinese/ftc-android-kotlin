package com.ft.ftchinese.ui.wxlink.merge

import android.content.res.Resources
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.request.WxLinkParams
import com.ft.ftchinese.repository.AccountRepo
import com.ft.ftchinese.repository.LinkRepo
import com.ft.ftchinese.ui.util.ConnectionState
import com.ft.ftchinese.ui.util.connectivityState
import com.ft.ftchinese.ui.components.BaseState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MergeState(
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
    resources: Resources,
    connState: State<ConnectionState>
) : BaseState(scaffoldState, scope, resources, connState) {

    var accountLinked by mutableStateOf<Account?>(null)
        private set

    fun link(account: Account) {
        if (!ensureConnected()) {
            return
        }
        val unionId = account.unionId ?: return
        progress.value = true

        scope.launch {

            val doneResult = LinkRepo.asyncLink(
                unionId = unionId,
                params = WxLinkParams(
                    ftcId = account.id,
                ),
            )

            when (doneResult) {
                is FetchResult.LocalizedError -> {
                    progress.value = false
                    showSnackBar(doneResult.msgId)
                }
                is FetchResult.TextError -> {
                    progress.value = false
                    showSnackBar(doneResult.text)
                }
                is FetchResult.Success -> {
                    refresh(account)
                }
            }
        }
    }

    private suspend fun refresh(account: Account) {
        when (val result = AccountRepo.asyncRefresh(account)) {
            is FetchResult.LocalizedError -> {
                showSnackBar(result.msgId)
            }
            is FetchResult.TextError -> {
                showSnackBar(result.text)
            }
            is FetchResult.Success -> {
                accountLinked = result.data
            }
        }
        progress.value = false
    }


}

@Composable
fun rememberMergeState(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    scope: CoroutineScope = rememberCoroutineScope(),
    resources: Resources = LocalContext.current.resources,
    connState: State<ConnectionState> = connectivityState()
) = remember(scaffoldState, scope, resources) {
    MergeState(
        scaffoldState = scaffoldState,
        scope = scope,
        resources = resources,
        connState,
    )
}
