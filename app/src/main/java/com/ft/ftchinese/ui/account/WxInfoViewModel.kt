package com.ft.ftchinese.ui.account

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.fetch.ServerError
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.WxSession
import com.ft.ftchinese.repository.AccountRepo
import com.ft.ftchinese.ui.base.BaseViewModel
import com.ft.ftchinese.ui.data.ApiRequest
import com.ft.ftchinese.viewmodel.WxRefreshState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WxInfoViewModel : BaseViewModel() {
    val swipingLiveData = MutableLiveData(false)

    val isButtonEnabled = MediatorLiveData<Boolean>().apply {
        value = true
        addSource(progressLiveData) {
            value = !it && (swipingLiveData.value == false)
        }
        addSource(swipingLiveData) {
            value == !it && (progressLiveData.value == false)
        }
    }

    val accountLoaded: MutableLiveData<FetchResult<Account>> by lazy {
        MutableLiveData<FetchResult<Account>>()
    }

    val sessionState: MutableLiveData<WxRefreshState> by lazy {
        MutableLiveData<WxRefreshState>()
    }

    // Refresh wechat info using current session.
    // The session might be expired.
    // Retrieve account only after session refreshed.
    fun refresh(account: Account, sess: WxSession) {
        if (isNetworkAvailable.value == false) {
            accountLoaded.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        swipingLiveData.value = true
        viewModelScope.launch {
            try {
                val done = withContext(Dispatchers.IO) {
                    AccountRepo.refreshWxInfo(wxSession = sess)
                }

                if (done) {
                    sessionState.value = WxRefreshState.SUCCESS
                    accountLoaded.value = ApiRequest.asyncRefreshAccount(account)
                    // TODO: also refresh avatar.
                } else {
                    sessionState.value = WxRefreshState.ReAuth
                }

                swipingLiveData.value = false
            } catch (e: ServerError) {
                swipingLiveData.value = false
                accountLoaded.value = when (e.statusCode) {
                    404 -> FetchResult.LocalizedError(R.string.account_not_found)
                    // TODO: handle 422?
                    else -> FetchResult.fromServerError(e)
                }
            } catch (e: Exception) {
                swipingLiveData.value = false
                accountLoaded.value = FetchResult.fromException(e)
            }
        }
    }
}
