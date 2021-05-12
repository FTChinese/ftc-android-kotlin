package com.ft.ftchinese.ui.wxlink

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.ClientError
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.repository.LinkRepo
import com.ft.ftchinese.ui.base.BaseViewModel
import com.ft.ftchinese.ui.data.ApiRequest
import com.ft.ftchinese.model.fetch.FetchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LinkViewModel : BaseViewModel() {

    val linkableLiveData = MutableLiveData<Account>()

    val isFormEnabled = MediatorLiveData<Boolean>().apply {
        addSource(progressLiveData) {
            value = enableSubmit()
        }
        addSource(linkableLiveData) {
            value = enableSubmit()
        }
    }

    private fun enableSubmit(): Boolean {
        return (progressLiveData.value == false) && (linkableLiveData.value == null) && (accountLinked.value !is FetchResult.Success)
    }

    init {
        progressLiveData.value = false
    }

    val accountLinked: MutableLiveData<FetchResult<Account>> by lazy {
        MutableLiveData<FetchResult<Account>>()
    }

    fun link() {
        if (isNetworkAvailable.value == false) {
            accountLinked.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        val linkedAccount = linkableLiveData.value ?: return
        linkedAccount.unionId ?: return

        progressLiveData.value = true

        viewModelScope.launch {
            try {
                val done = withContext(Dispatchers.IO) {
                    LinkRepo.link(
                        ftcId = linkedAccount.id,
                        unionId = linkedAccount.unionId
                    )
                }

                if (done) {
                    accountLinked.value = ApiRequest.asyncRefreshAccount(linkedAccount)
                    progressLiveData.value = false
                } else {
                    progressLiveData.value = false
                    accountLinked.value = FetchResult.LocalizedError(R.string.loading_failed)
                }
            } catch (e: ClientError) {
                val msgId = when(e.statusCode) {
                    404 -> R.string.account_not_found
                    422 -> when (e.error?.key) {
                        "account_link_already_taken" -> R.string.api_account_already_linked
                        "membership_link_already_taken" -> R.string.api_membership_already_linked
                        "membership_all_valid" -> R.string.api_membership_all_valid
                        else -> null
                    }
                    else -> null
                }

                progressLiveData.value = false

                accountLinked.value = if (msgId != null) {
                    FetchResult.LocalizedError(msgId)
                } else {
                    FetchResult.fromServerError(e)
                }

            } catch (e: Exception) {
                progressLiveData.value = false
                accountLinked.value = FetchResult.fromException(e)
            }
        }
    }
}
