package com.ft.ftchinese.ui.components

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.mutableStateOf
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.repository.AccountRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

open class BaseState(
    val scaffoldState: ScaffoldState,
    val scope: CoroutineScope,
    private val resources: Resources
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
