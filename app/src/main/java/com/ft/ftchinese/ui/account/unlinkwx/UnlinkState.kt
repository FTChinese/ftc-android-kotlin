package com.ft.ftchinese.ui.account.unlinkwx

import android.content.res.Resources
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.UnlinkAnchor
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.request.WxUnlinkParams
import com.ft.ftchinese.repository.AccountRepo
import com.ft.ftchinese.repository.LinkRepo
import com.ft.ftchinese.ui.util.ConnectionState
import com.ft.ftchinese.ui.util.connectivityState
import com.ft.ftchinese.ui.components.BaseState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class UnlinkState(
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
    resources: Resources,
    connState: State<ConnectionState>
) : BaseState(scaffoldState, scope, resources, connState) {

    var accountUnlinked by mutableStateOf<Account?>(null)
        private set

    fun unlink(account: Account, anchor: UnlinkAnchor) {
        if (!ensureConnected()) {
            return
        }
        val unionId = account.unionId
        if (unionId == null) {
            showSnackBar(R.string.unlink_missing_union_id)
            return
        }

        progress.value = true

        val params = WxUnlinkParams(
            ftcId = account.id,
            anchor = anchor,
        )

        scope.launch {
            val doneResult = LinkRepo.asyncUnlink(
                unionId = unionId,
                params = params,
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
                accountUnlinked = result.data
            }
        }
        progress.value = false
    }

}

@Composable
fun rememberUnlinkState(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    scope: CoroutineScope = rememberCoroutineScope(),
    resources: Resources = LocalContext.current.resources,
    connState: State<ConnectionState> = connectivityState()
) = remember(scaffoldState, scope, resources) {
    UnlinkState(
        scaffoldState = scaffoldState,
        scope = scope,
        resources = resources,
        connState,
    )
}
