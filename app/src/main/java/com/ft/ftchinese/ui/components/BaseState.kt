package com.ft.ftchinese.ui.components

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.mutableStateOf
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.repository.AccountRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

open class BaseState(
    val scaffoldState: ScaffoldState,
    val scope: CoroutineScope,
    val resources: Resources
) {
    val progress = mutableStateOf(false)
    val accountRefreshed = mutableStateOf<Account?>(null)

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

    fun showNotConnected() {
        showSnackBar(resources.getString(R.string.prompt_no_network))
    }

    suspend fun refresh(account: Account) {
        accountRefreshed.value = null
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
                accountRefreshed.value = refreshed.data
            }
        }
    }
}
