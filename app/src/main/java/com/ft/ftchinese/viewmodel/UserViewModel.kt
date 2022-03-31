package com.ft.ftchinese.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.APIError
import com.ft.ftchinese.model.fetch.FetchUi
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.repository.StripeClient
import com.ft.ftchinese.store.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserViewModel(application: Application) : AndroidViewModel(application) {
    private val session = SessionManager.getInstance(application)

    var account by mutableStateOf<Account?>(null)
        private set

    init {
        account = session.loadAccount(raw = true)
    }

    val isLoggedIn: Boolean
        get() = account != null

    val isWxOnly: Boolean
        get() = account?.isWxOnly == true

    val progressLiveData: MutableLiveData<FetchUi> by lazy {
        MutableLiveData<FetchUi>()
    }

    fun load() {
        account = session.loadAccount(raw = true)
    }

    fun save(a: Account) {
        account = a
        session.saveAccount(a)
    }

    fun saveMembership(m: Membership) {
        account = account?.withMembership(m)
        session.saveMembership(m)
    }

    fun clear() {
        account = null
        session.logout()
    }

    fun createCustomer(account: Account) {
        progressLiveData.value = FetchUi.Progress(true)

        viewModelScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) {
                    StripeClient.createCustomer(account)
                }

                if (resp.body == null) {
                    progressLiveData.value = FetchUi.ResMsg(R.string.stripe_customer_not_created)
                    return@launch
                }

                progressLiveData.value = FetchUi.Progress(false)
                save(account.withCustomerID(resp.body.id))
            } catch (e: APIError) {

                progressLiveData.value = if (e.statusCode == 404) {
                    FetchUi.ResMsg(R.string.stripe_customer_not_found)
                } else {
                    FetchUi.fromApi(e)
                }
            } catch (e: Exception) {
                progressLiveData.value = FetchUi.fromException(e)
            }
        }
    }
}
