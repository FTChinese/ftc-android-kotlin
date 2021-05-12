package com.ft.ftchinese.ui.wxlink

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.ServerError
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.repository.LinkRepo
import com.ft.ftchinese.ui.base.BaseViewModel
import com.ft.ftchinese.ui.data.ApiRequest
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.request.WxLinkParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class LinkViewModel : BaseViewModel(), AnkoLogger {

    // The linked account calculcated on user device.
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
        if (progressLiveData.value == true) {
            info("")
            return false
        }

        // Linked account is calculated on device.
        return linkableLiveData.value != null
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
                        unionId = linkedAccount.unionId,
                        params = WxLinkParams(
                            ftcId = linkedAccount.id,
                        ),
                    )
                }

                progressLiveData.value = false

                if (done) {
                    accountLinked.value = ApiRequest.asyncRefreshAccount(linkedAccount)
                    isFormEnabled.value = false
                } else {
                    accountLinked.value = FetchResult.LocalizedError(R.string.loading_failed)
                }
            } catch (e: ServerError) {
                progressLiveData.value = false
                handleServerError(e)
            } catch (e: Exception) {
                progressLiveData.value = false
                accountLinked.value = FetchResult.fromException(e)
            }
        }
    }

    private fun handleServerError(e: ServerError) {
        accountLinked.value = when (e.statusCode) {
            404 -> FetchResult.LocalizedError(R.string.account_not_found)
            422 -> if (e.error == null) {
                FetchResult.fromServerError(e)
            } else {
                when {
                    e.error.isFieldAlreadyExists("account_link") -> FetchResult.LocalizedError(R.string.api_account_already_linked)
                    e.error.isFieldAlreadyExists("membership_link") -> FetchResult.LocalizedError(R.string.api_membership_already_linked)
                    e.error.isFieldAlreadyExists("membership_both_valid") -> FetchResult.LocalizedError(R.string.api_membership_all_valid)
                    else -> FetchResult.fromServerError(e)
                }
            }
            else -> FetchResult.fromServerError(e)
        }
    }
}
