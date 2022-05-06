package com.ft.ftchinese.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.ui.base.BaseViewModel
import com.ft.ftchinese.ui.data.ApiRequest
import kotlinx.coroutines.launch

private const val TAG = "AccountViewModel"

@Deprecated("")
class AccountViewModel : BaseViewModel() {

    val accountRefreshed: MutableLiveData<FetchResult<Account>> by lazy {
        MutableLiveData<FetchResult<Account>>()
    }

    // Refresh a user's account data, regardless of logged in
    // via email or wecaht.
    fun refresh(account: Account, manual: Boolean = false) {
        if (isNetworkAvailable.value == false) {
            accountRefreshed.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        if (!manual) {
            progressLiveData.value = true
        }
        viewModelScope.launch {
            Log.i(TAG, "Start refreshing account")

            accountRefreshed.value = ApiRequest
                .asyncRefreshAccount(account)

            progressLiveData.value = false
        }
    }

}
