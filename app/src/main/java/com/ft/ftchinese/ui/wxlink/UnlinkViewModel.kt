package com.ft.ftchinese.ui.wxlink

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.ClientError
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.UnlinkAnchor
import com.ft.ftchinese.model.request.WxUnlinkParams
import com.ft.ftchinese.repository.LinkRepo
import com.ft.ftchinese.ui.base.BaseViewModel
import com.ft.ftchinese.ui.data.FetchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UnlinkViewModel : BaseViewModel() {

    val anchorSelected = MutableLiveData<UnlinkAnchor>()

    val unlinkResult: MutableLiveData<FetchResult<Boolean>> by lazy {
        MutableLiveData<FetchResult<Boolean>>()
    }

    fun selectAnchor(anchor: UnlinkAnchor) {
        anchorSelected.value = anchor
    }

    fun unlink(account: Account) {
        if (isNetworkAvailable.value == false) {
            unlinkResult.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        val anchor = anchorSelected.value

        if (account.isMember && anchor == null) {
            unlinkResult.value = FetchResult.LocalizedError(R.string.api_anchor_missing)
            return
        }

        if (account.unionId == null) {
            unlinkResult.value = FetchResult.LocalizedError(R.string.unlink_missing_union_id)
            return
        }

        val params = WxUnlinkParams(
            ftcId = account.id,
            anchor = anchor,
        )

        progressLiveData.value = true
        viewModelScope.launch {
            try {
                val done = withContext(Dispatchers.IO) {
                    LinkRepo.unlink(account.unionId, params)
                }

                unlinkResult.value = FetchResult.Success(done)
            } catch (e: ClientError) {
                val msgId = when (e.statusCode) {
                    422 -> when (e.error?.key) {
                        "anchor_missing_field" -> R.string.api_anchor_missing
                        else -> null
                    }
                    404 -> R.string.account_not_found
                    else -> null
                }

                unlinkResult.value = if (msgId != null) {
                    FetchResult.LocalizedError(msgId)
                } else {
                    FetchResult.fromServerError(e)
                }
                progressLiveData.value = false
            } catch (e: Exception) {
                unlinkResult.value = FetchResult.fromException(e)
                progressLiveData.value = false
            }
        }
    }
}
