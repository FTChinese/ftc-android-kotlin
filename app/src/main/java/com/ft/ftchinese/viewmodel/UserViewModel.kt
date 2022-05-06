package com.ft.ftchinese.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.APIError
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.repository.AccountRepo
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.components.ToastMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "UserViewModel"

open class UserViewModel(application: Application) : BaseAppViewModel(application) {
    protected val session = SessionManager.getInstance(application)

    var account by mutableStateOf<Account?>(null)
        private set

    init {
        account = session.loadAccount(raw = true)
    }

    val isLoggedIn: Boolean
        get() = account != null

    val isWxOnly: Boolean
        get() = account?.isWxOnly == true

    fun reloadAccount() {
        account = session.loadAccount(raw = true)
    }

    fun saveAccount(a: Account) {
        account = a
        session.saveAccount(a)
    }

    fun saveMembership(m: Membership) {
        account = account?.withMembership(m)
        session.saveMembership(m)
    }

    fun logout() {
        account = null
        session.logout()
    }

    fun refreshAccount() {
        val a = account ?: return

        if (!ensureNetwork()) {
            return
        }

        toastLiveData.value = ToastMessage.Resource(R.string.refreshing_account)

        refreshingLiveData.value = true
        viewModelScope.launch {
            Log.i(TAG, "Start refreshing account")

            asyncRefreshAccount(a)?.let {
                saveAccount(it)
            }

            refreshingLiveData.value = false
        }
    }

    protected suspend fun asyncRefreshAccount(a: Account): Account? {
        try {
            val refreshed = withContext(Dispatchers.IO) {
                AccountRepo.refresh(a)
            }

            return if (refreshed == null) {
                toastLiveData.value = ToastMessage.Resource(R.string.loading_failed)
                null
            } else {
                toastLiveData.value = ToastMessage.Resource(R.string.refresh_success)
                refreshed
            }
        } catch (e: APIError) {
            toastLiveData.value = if (e.statusCode == 404) {
                ToastMessage.Resource(R.string.account_not_found)
            } else {
                ToastMessage.fromApi(e)
            }

            return null
        } catch (e: Exception) {
            toastLiveData.value = ToastMessage.fromException(e)
            return null
        }
    }
}
