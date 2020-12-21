package com.ft.ftchinese.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.UnlinkAnchor
import com.ft.ftchinese.repository.LinkRepo
import com.ft.ftchinese.model.fetch.ClientError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LinkViewModel : ViewModel() {

    val unlinkResult: MutableLiveData<Result<Boolean>> by lazy {
        MutableLiveData<Result<Boolean>>()
    }

    val linkResult: MutableLiveData<Result<Boolean>> by lazy {
        MutableLiveData<Result<Boolean>>()
    }

    val anchorSelected = MutableLiveData<UnlinkAnchor>()

    fun link(ftcId: String, unionId: String) {
        viewModelScope.launch {
            try {
                val done = withContext(Dispatchers.IO) {
                    LinkRepo.link(
                            ftcId = ftcId,
                            unionId = unionId
                    )
                }

                linkResult.value = Result.Success(done)

            } catch (e: ClientError) {
                val msgId = when(e.statusCode) {
                    404 -> R.string.api_account_not_found
                    422 -> when (e.error?.key) {
                        "account_link_already_taken" -> R.string.api_account_already_linked
                        "membership_link_already_taken" -> R.string.api_membership_already_linked
                        "membership_all_valid" -> R.string.api_membership_all_valid
                        else -> null
                    }
                    else -> null
                }


                linkResult.value = if (msgId != null) {
                    Result.LocalizedError(msgId)
                } else {
                    parseApiError(e)
                }

            } catch (e: Exception) {
                linkResult.value = parseException(e)
            }
        }
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

                unlinkResult.value = Result.Success(done)

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
                    Result.LocalizedError(msgId)
                } else {
                    parseApiError(e)
                }

            } catch (e: Exception) {
                unlinkResult.value = parseException(e)

            }
        }
    }
}
