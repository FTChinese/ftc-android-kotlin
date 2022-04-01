package com.ft.ftchinese.ui.wxlink

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.APIError
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.UnlinkAnchor
import com.ft.ftchinese.model.request.WxUnlinkParams
import com.ft.ftchinese.repository.LinkRepo
import com.ft.ftchinese.ui.base.BaseViewModel
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.ui.data.ApiRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "UnlinkViewModel"

class UnlinkViewModel : BaseViewModel() {

    private val anchorSelected = MutableLiveData<UnlinkAnchor>()

    fun selectAnchor(anchor: UnlinkAnchor) {
        Log.i(TAG, "$anchor")
        anchorSelected.value = anchor
    }

    val accountLoaded: MutableLiveData<FetchResult<Account>> by lazy {
        MutableLiveData<FetchResult<Account>>()
    }

    fun unlink(account: Account) {
        if (isNetworkAvailable.value == false) {
            accountLoaded.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        val anchor = anchorSelected.value

        if (account.isMember && anchor == null) {
            accountLoaded.value = FetchResult.LocalizedError(R.string.api_anchor_missing)
            return
        }

        if (account.unionId == null) {
            accountLoaded.value = FetchResult.LocalizedError(R.string.unlink_missing_union_id)
            return
        }

        val params = WxUnlinkParams(
            ftcId = account.id,
            anchor = anchor,
        )

        Log.i(TAG, "$params")

        progressLiveData.value = true
        viewModelScope.launch {
            try {
                val done = withContext(Dispatchers.IO) {
                    LinkRepo.unlink(account.unionId, params)
                }

                if (done) {
                    accountLoaded.value = ApiRequest.asyncRefreshAccount(account)
                } else {
                    accountLoaded.value = FetchResult.LocalizedError(R.string.loading_failed)
                }
                progressLiveData.value = false
            } catch (e: APIError) {
                val msgId = when (e.statusCode) {
                    422 -> if (e.error?.isFieldMissing("anchor") == true) {
                        R.string.api_anchor_missing
                    } else null
                    404 -> R.string.account_not_found
                    else -> null
                }

                accountLoaded.value = if (msgId != null) {
                    FetchResult.LocalizedError(msgId)
                } else {
                    FetchResult.fromApi(e)
                }
                progressLiveData.value = false
            } catch (e: Exception) {
                accountLoaded.value = FetchResult.fromException(e)
                progressLiveData.value = false
            }
        }
    }
}
