package com.ft.ftchinese.ui.wxlink

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.request.WxLinkParams
import com.ft.ftchinese.repository.AccountRepo
import com.ft.ftchinese.repository.LinkRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class LinkState(
    val scaffoldState: ScaffoldState,
    val scope: CoroutineScope,
    private val resources: Resources
) {

    val progress = mutableStateOf(false)
    val linked = mutableStateOf<Account?>(null)

    fun showSnackBar(@StringRes id: Int) {
        try {
            showSnackBar(resources.getString(id))
        } catch (e: Exception) {

        }
    }

    fun showSnackBar(message: String) {
        scope.launch {
            scaffoldState.snackbarHostState.showSnackbar(message)
        }
    }

    fun link(account: Account) {
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
        when (val refreshed = AccountRepo.asyncRefresh(account)) {
            is FetchResult.LocalizedError -> {
                progress.value = false
                showSnackBar(refreshed.msgId)
            }
            is FetchResult.TextError -> {
                progress.value = false
                showSnackBar(refreshed.text)
            }
            is FetchResult.Success -> {
                progress.value = false
                linked.value = refreshed.data
            }
        }
    }
}

@Composable
fun rememberLinkState(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    scope: CoroutineScope = rememberCoroutineScope(),
    resources: Resources = LocalContext.current.resources
) = remember(scaffoldState, scope, resources) {
    LinkState(
        scaffoldState = scaffoldState,
        scope = scope,
        resources = resources
    )
}
