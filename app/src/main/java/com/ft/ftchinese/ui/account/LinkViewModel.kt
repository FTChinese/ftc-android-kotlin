package com.ft.ftchinese.ui.account

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.Account
import com.ft.ftchinese.model.FtcUser
import com.ft.ftchinese.model.UnlinkAnchor
import com.ft.ftchinese.util.ClientError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LinkViewModel : ViewModel() {

    val unlinkResult = MutableLiveData<BinaryResult>()
    val linkResult = MutableLiveData<BinaryResult>()
    val anchorSelected = MutableLiveData<UnlinkAnchor>()

    fun link(ftcId: String, unionId: String) {
        viewModelScope.launch {
            try {
                val done = withContext(Dispatchers.IO) {
                    FtcUser(ftcId).linkWechat(unionId)
                }

                linkResult.value = BinaryResult(
                        success = done
                )
            } catch (e: ClientError) {
                val msgId = when(e.statusCode) {
                    404 -> R.string.api_account_not_found
                    422 -> when (e.error?.key) {
                        "account_link_already_taken" -> R.string.api_account_already_linked
                        "membership_link_already_taken" -> R.string.api_membership_already_linked
                        "membership_all_valid" -> R.string.api_membership_all_valid
                        else -> null
                    }
                    else -> e.parseStatusCode()
                }

                linkResult.value = BinaryResult(
                        error = msgId,
                        exception = e
                )
            } catch (e: Exception) {
                linkResult.value = BinaryResult(
                        exception = e
                )
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
                    account.unlink(anchor)
                }

                unlinkResult.value = BinaryResult(
                        success = done
                )
            } catch (e: ClientError) {
                val msgId = when (e.statusCode) {
                    422 -> when (e.error?.key) {
                        "anchor_missing_field" -> R.string.api_anchor_missing
                        else -> null
                    }
                    404 -> R.string.api_account_not_found
                    else -> e.parseStatusCode()
                }

                unlinkResult.value = BinaryResult(
                        error = msgId,
                        exception = e
                )
            } catch (e: Exception) {
                unlinkResult.value = BinaryResult(
                        exception = e
                )
            }
        }
    }
}
