package com.ft.ftchinese.ui.account

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.viewmodel.Result
import com.ft.ftchinese.model.reader.*
import com.ft.ftchinese.model.subscription.StripeCustomer
import com.ft.ftchinese.repository.AccountRepo
import com.ft.ftchinese.repository.StripeRepo
import com.ft.ftchinese.ui.launch.AvatarResult
import com.ft.ftchinese.ui.login.AccountResult
import com.ft.ftchinese.util.ClientError
import com.ft.ftchinese.util.FileCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import java.io.ByteArrayInputStream

class AccountViewModel : ViewModel(), AnkoLogger {

    val inProgress = MutableLiveData<Boolean>()
    val uiType = MutableLiveData<LoginMethod>()
    val shouldReAuth = MutableLiveData<Boolean>()

    val accountRefreshed: MutableLiveData<AccountResult> by lazy {
        MutableLiveData<AccountResult>()
    }

    val avatarRetrieved: MutableLiveData<AvatarResult> by lazy {
        MutableLiveData<AvatarResult>()
    }

    val wxRefreshResult: MutableLiveData<WxRefreshResult> by lazy {
        MutableLiveData<WxRefreshResult>()
    }

//    val customerIdResult: MutableLiveData<StringResult> by lazy {
//        MutableLiveData<StringResult>()
//    }

    val customerResult: MutableLiveData<Result<StripeCustomer>> by lazy {
        MutableLiveData<Result<StripeCustomer>>()
    }

    val stripeRetrievalResult: MutableLiveData<StripeRetrievalResult> by lazy {
        MutableLiveData<StripeRetrievalResult>()
    }

    // Ask API to fetch user's latest wechat info and save
    // it to database.
    fun refreshWxInfo(wxSession: WxSession) {
        viewModelScope.launch {
            try {
                val done = withContext(Dispatchers.IO) {
                    AccountRepo.refreshWxInfo(wxSession)
                }

                wxRefreshResult.value = WxRefreshResult(
                        success = done
                )
            } catch (e: ClientError) {
                if (e.statusCode == 422) {
                    wxRefreshResult.value = WxRefreshResult(
                            isExpired = true
                    )
                    return@launch
                }

                wxRefreshResult.value = WxRefreshResult(
                        error = when (e.statusCode) {
                            404 -> R.string.api_account_not_found
                            else -> null
                        },
                        exception = e
                )

            } catch (e: Exception) {
                wxRefreshResult.value = WxRefreshResult(
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
                    AccountRepo.refresh(account)
                }

                accountRefreshed.value = AccountResult(
                        success = updatedAccount ?: account
                )
            } catch (e: ClientError) {
                val msgId = if (e.statusCode == 404) {
                    R.string.api_account_not_found
                } else {
                    e.parseStatusCode()
                }

                accountRefreshed.value = AccountResult(
                        error = msgId,
                        exception = e
                )
            } catch (e: Exception) {

                accountRefreshed.value = AccountResult(
                        exception = e
                )
            }
        }
    }

    fun fetchWxAvatar(cache: FileCache, wechat: Wechat) {
        if (wechat.avatarUrl == null) {
            // TODO: display error message.
            return
        }
        viewModelScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    AccountRepo.loadWxAvatar(wechat.avatarUrl)
                } ?: return@launch

                avatarRetrieved.value = AvatarResult(
                        success = ByteArrayInputStream(bytes)
                )

                withContext(Dispatchers.IO) {
                    cache.writeBinaryFile(
                            WX_AVATAR_NAME,
                            bytes
                    )
                }

            } catch (e: Exception) {

                avatarRetrieved.value = AvatarResult(
                        exception = e
                )
            }
        }
    }

    fun createCustomer(account: Account) {
        viewModelScope.launch {
            try {
                val customer = withContext(Dispatchers.IO) {
                    StripeRepo.createCustomer(account.id)
                }

                if (customer == null) {
//                    customerIdResult.value = StringResult(
//                            error = R.string.stripe_customer_not_created
//                    )
                    customerResult.value = Result.LocalizedError(R.string.stripe_customer_not_created)
                    return@launch
                }
//
//                customerIdResult.value = StringResult(
//                        success = id
//                )
                customerResult.value = Result.Success(customer)

            } catch (e: ClientError) {
//                val msgId = when (e.statusCode) {
//                    400 -> R.string.stripe_customer_not_found
//                    else -> null
//                }
//                customerIdResult.value = StringResult(
//                        error = msgId,
//                        exception = e
//                )
                if (e.statusCode == 404) {
                    customerResult.value = Result.LocalizedError(R.string.stripe_customer_not_found)
                } else {
                    customerResult.value = Result.Error(e)
                }
            } catch (e: Exception) {
//                customerIdResult.value = StringResult(
//                        exception = e
//                )
                customerResult.value = Result.Error(e)
            }

        }
    }

    fun retrieveStripeSub(account: Account) {
        viewModelScope.launch {
            try {
                val stripeSub = withContext(Dispatchers.IO) {
                    StripeRepo.refreshStripeSub(account)
                }

                stripeRetrievalResult.value = StripeRetrievalResult(
                        success = stripeSub
                )

            } catch (e: ClientError) {
                if (e.statusCode == 404) {
                    stripeRetrievalResult.value = StripeRetrievalResult(
                            success = null
                    )

                    return@launch
                }

                stripeRetrievalResult.value = StripeRetrievalResult(
                        exception = e
                )
            } catch (e: Exception) {
                stripeRetrievalResult.value = StripeRetrievalResult(
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
