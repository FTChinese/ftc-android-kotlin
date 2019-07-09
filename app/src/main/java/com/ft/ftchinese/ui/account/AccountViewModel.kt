package com.ft.ftchinese.ui.account

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.*
import com.ft.ftchinese.ui.StringResult
import com.ft.ftchinese.ui.login.AccountResult
import com.ft.ftchinese.util.ClientError
import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.util.FileCache
import com.ft.ftchinese.util.StripeError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class AccountViewModel : ViewModel(), AnkoLogger {

    val inProgress = MutableLiveData<Boolean>()
    val uiType = MutableLiveData<LoginMethod>()
    val shouldReAuth = MutableLiveData<Boolean>()

    val sendEmailResult = MutableLiveData<BinaryResult>()

    private val _accountRefreshed = MutableLiveData<AccountResult>()
    val accountRefreshed: LiveData<AccountResult> = _accountRefreshed

    private val _avatarResult = MutableLiveData<ImageResult>()
    val avatarResult: LiveData<ImageResult> = _avatarResult

    private val _wxRefreshResult = MutableLiveData<WxRefreshResult>()
    val wxRefreshResult: LiveData<WxRefreshResult> = _wxRefreshResult

    val customerIdResult = MutableLiveData<StringResult>()

    // Ask API to fetch user's latest wechat info and save
    // it to database.
    fun refreshWxInfo(wxSession: WxSession) {
        viewModelScope.launch {
            try {
                val done = withContext(Dispatchers.IO) {
                    wxSession.refreshInfo()
                }

                _wxRefreshResult.value = WxRefreshResult(
                        success = done
                )
            } catch (e: ClientError) {
                if (e.statusCode == 422) {
                    _wxRefreshResult.value = WxRefreshResult(
                            isExpired = true
                    )
                    return@launch
                }

                _wxRefreshResult.value = WxRefreshResult(
                        error = when (e.statusCode) {
                            404 -> R.string.api_account_not_found
                            else -> null
                        },
                        exception = e
                )

            } catch (e: Exception) {
                _wxRefreshResult.value = WxRefreshResult(
                        exception = e
                )
            }
        }
    }

    // Refresh a user's account data, regardless of logged in
    // via email or wecaht.
    fun refresh(account: Account) {
        viewModelScope.launch {
            info("Start refreshing account")

            try {

                val updatedAccount = withContext(Dispatchers.IO) {
                    account.refresh()
                }

                _accountRefreshed.value = AccountResult(
                        success = updatedAccount ?: account
                )
            } catch (e: ClientError) {
                val msgId = if (e.statusCode == 404) {
                    R.string.api_account_not_found
                } else {
                    e.statusMessage()
                }

                _accountRefreshed.value = AccountResult(
                        error = msgId,
                        exception = e
                )
            } catch (e: Exception) {

                _accountRefreshed.value = AccountResult(
                        exception = e
                )
            }
        }
    }

    fun fetchWxAvatar(cache: FileCache, wechat: Wechat) {
        val url = wechat.avatarUrl ?:  return
        viewModelScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    Fetch()
                            .get(url)
                            .download()
                }

                _avatarResult.value = ImageResult(
                        success = bytes
                )

                if (bytes == null) {
                    return@launch
                }

                cache.writeBinaryFile(
                        WX_AVATAR_NAME,
                        bytes
                )

            } catch (e: Exception) {
                _avatarResult.value = ImageResult(
                        exception = e
                )
            }
        }
    }

    fun requestVerification(userId: String) {
        viewModelScope.launch {
            try {
                val done = withContext(Dispatchers.IO) {
                    FtcUser(userId).requestVerification()
                }

                sendEmailResult.value = BinaryResult(
                        success = done
                )

            } catch (e: ClientError) {
                val msgId = when (e.statusCode) {
                    404 -> R.string.api_account_not_found
                    422 -> when (e.error?.key) {
                        "email_server_missing" -> R.string.api_email_server_down
                        else -> null
                    }
                    else -> e.statusMessage()
                }

                sendEmailResult.value = BinaryResult(
                        error = msgId,
                        exception = e
                )

            } catch (e: Exception) {
                sendEmailResult.value = BinaryResult(
                        exception = e
                )
            }
        }
    }

    fun createCustomer(account: Account) {
        viewModelScope.launch {
            try {
                val id = withContext(Dispatchers.IO) {
                    account.createCustomer()
                }

                if (id == null) {
                    customerIdResult.value = StringResult(
                            error = R.string.stripe_customer_not_created
                    )
                    return@launch
                }

                customerIdResult.value = StringResult(
                        success = id
                )
            } catch (e: StripeError) {
                val msgId = when (e.status) {
                    400 -> R.string.stripe_customer_not_found
                    else -> null
                }
                customerIdResult.value = StringResult(
                        error = msgId,
                        exception = e
                )
            } catch (e: Exception) {
                customerIdResult.value = StringResult(
                        exception = e
                )
            }

        }
    }

    fun showProgress(show: Boolean) {
        inProgress.value = show
    }

    fun switchUI(m: LoginMethod) {
        uiType.value = m
    }

    // Show re-authorzation dialog for wechat.
    fun showReAuth() {
        shouldReAuth.value = true
    }
}
