package com.ft.ftchinese.ui.wxlink

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.ClientError
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.UnlinkAnchor
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

    fun unlink(account: Account, anchor: UnlinkAnchor? = null) {
        viewModelScope.launch {
            try {
                val done = withContext(Dispatchers.IO) {
                    LinkRepo.unlink(account, anchor)
                }

                unlinkResult.value = FetchResult.Success(done)

            } catch (e: ClientError) {
                val msgId = when (e.statusCode) {
                    422 -> when (e.error?.key) {
                        "anchor_missing_field" -> R.string.api_anchor_missing
                        else -> null
                    }
                    404 -> R.string.api_account_not_found
                    else -> null
                }

                unlinkResult.value = if (msgId != null) {
                    FetchResult.LocalizedError(msgId)
                } else {
                    FetchResult.fromServerError(e)
                }

            } catch (e: Exception) {
                unlinkResult.value = FetchResult.fromException(e)

            }
        }
    }
}
