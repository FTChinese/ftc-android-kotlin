package com.ft.ftchinese.ui.auth

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.repository.AccountRepo
import com.ft.ftchinese.viewmodel.UserViewModel
import kotlinx.coroutines.launch

fun ComponentActivity.validateSessionOnLaunch(
    userViewModel: UserViewModel,
    logTag: String,
) {
    val account = userViewModel.account ?: return

    lifecycleScope.launch {
        when (val result = AccountRepo.asyncRefresh(account)) {
            is FetchResult.Success -> {
                userViewModel.saveAccount(result.data)
                Log.i(logTag, "Validated session on launch for ${account.id}")
            }
            is FetchResult.LocalizedError -> {
                Log.i(logTag, "Launch session validation returned localized error ${result.msgId}")
            }
            is FetchResult.TextError -> {
                Log.i(logTag, "Launch session validation returned text error ${result.text}")
            }
        }
    }
}
