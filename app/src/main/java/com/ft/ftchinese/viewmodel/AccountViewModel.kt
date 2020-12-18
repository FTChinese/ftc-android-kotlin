package com.ft.ftchinese.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.order.StripeSubResult
import com.ft.ftchinese.model.reader.*
import com.ft.ftchinese.model.subscription.IAPSubs
import com.ft.ftchinese.model.subscription.Order
import com.ft.ftchinese.model.subscription.StripeCustomer
import com.ft.ftchinese.repository.AccountRepo
import com.ft.ftchinese.repository.ClientError
import com.ft.ftchinese.repository.StripeClient
import com.ft.ftchinese.repository.SubRepo
import com.ft.ftchinese.store.FileCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import java.io.ByteArrayInputStream
import java.io.InputStream

class AccountViewModel : ViewModel(), AnkoLogger {

    val inProgress: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    val isNetworkAvailable = MutableLiveData<Boolean>()

    val serviceAccepted: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    val uiType = MutableLiveData<LoginMethod>()

    val accountRefreshed: MutableLiveData<Result<Account>> by lazy {
        MutableLiveData<Result<Account>>()
    }

    val avatarRetrieved: MutableLiveData<Result<InputStream>> by lazy {
        MutableLiveData<Result<InputStream>>()
    }

    val wxRefreshResult: MutableLiveData<Result<WxRefreshState>> by lazy {
        MutableLiveData<Result<WxRefreshState>>()
    }

    val customerResult: MutableLiveData<Result<StripeCustomer>> by lazy {
        MutableLiveData<Result<StripeCustomer>>()
    }

    val stripeResult: MutableLiveData<Result<StripeSubResult>> by lazy {
        MutableLiveData<Result<StripeSubResult>>()
    }

    val iapRefreshResult: MutableLiveData<Result<IAPSubs>> by lazy {
        MutableLiveData<Result<IAPSubs>>()
    }

    val ordersResult: MutableLiveData<Result<List<Order>>> by lazy {
        MutableLiveData<Result<List<Order>>>()
    }

    // Ask API to fetch user's latest wechat info and save
    // it to database.
    fun refreshWxInfo(wxSession: WxSession) {
        if (isNetworkAvailable.value == false) {
            wxRefreshResult.value = Result.LocalizedError(R.string.prompt_no_network)
            return
        }

        viewModelScope.launch {
            try {
                val done = withContext(Dispatchers.IO) {
                    AccountRepo.refreshWxInfo(wxSession)
                }

                info("Refresh wx info result: $done")
                wxRefreshResult.value = if (done) {
                    Result.Success(WxRefreshState.SUCCESS)
                } else {
                    Result.Success(WxRefreshState.ReAuth)
                }

            } catch (e: ClientError) {
                info("Refresh wx info api error: $e")
                wxRefreshResult.value = when (e.statusCode) {
                    422 -> Result.Success(WxRefreshState.ReAuth)
                    404 -> Result.LocalizedError(R.string.api_account_not_found)
                    else -> parseApiError(e)
                }

            } catch (e: Exception) {
                info("Refresh wx info exception: $e")
                wxRefreshResult.value = parseException(e)
            }
        }
    }

    // Refresh a user's account data, regardless of logged in
    // via email or wecaht.
    fun refresh(account: Account) {
        if (isNetworkAvailable.value == false) {
            accountRefreshed.value = Result.LocalizedError(R.string.prompt_no_network)
            return
        }
        viewModelScope.launch {
            info("Start refreshing account")

            try {

                val updatedAccount = withContext(Dispatchers.IO) {
                    AccountRepo.refresh(account)
                }

                if (updatedAccount == null) {
                    info("Refresh account returned empty data")
                    accountRefreshed.value = Result.LocalizedError(R.string.loading_failed)
                    return@launch
                }

                info("Refresh account success")
                accountRefreshed.value = Result.Success(updatedAccount)
            } catch (e: ClientError) {

                info("Refresh account api error $e")

                accountRefreshed.value = if (e.statusCode == 404) {
                    Result.LocalizedError(R.string.api_account_not_found)
                } else {
                    parseApiError(e)
                }

            } catch (e: Exception) {

                info("Refresh account exception $e")

                accountRefreshed.value = parseException(e)
            }
        }
    }

    fun fetchWxAvatar(cache: FileCache, wechat: Wechat) {
        if (wechat.avatarUrl == null) {
            info("Wx avatar url empty")
            return
        }

        if (isNetworkAvailable.value == false) {
            avatarRetrieved.value = Result.LocalizedError(R.string.prompt_no_network)
            return
        }

        viewModelScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    AccountRepo.loadWxAvatar(wechat.avatarUrl)
                } ?: return@launch

                avatarRetrieved.value = Result.Success(ByteArrayInputStream(bytes))

                withContext(Dispatchers.IO) {
                    cache.writeBinaryFile(
                            WX_AVATAR_NAME,
                            bytes
                    )
                }

            } catch (e: Exception) {

                avatarRetrieved.value = parseException(e)
            }
        }
    }

    fun createCustomer(account: Account) {
        if (isNetworkAvailable.value == false) {
            customerResult.value = Result.LocalizedError(R.string.prompt_no_network)
            return
        }

        viewModelScope.launch {
            try {
                val customer = withContext(Dispatchers.IO) {
                    StripeClient.createCustomer(account)
                }

                if (customer == null) {

                    customerResult.value = Result.LocalizedError(R.string.stripe_customer_not_created)
                    return@launch
                }

                customerResult.value = Result.Success(customer)

            } catch (e: ClientError) {

                customerResult.value = if (e.statusCode == 404) {
                    Result.LocalizedError(R.string.stripe_customer_not_found)
                } else {
                     parseApiError(e)
                }
            } catch (e: Exception) {

                customerResult.value = parseException(e)
            }

        }
    }

    // Ask the latest stripe subscription data.
    fun refreshStripe(account: Account) {
        if (isNetworkAvailable.value == false) {
            stripeResult.value = Result.LocalizedError(R.string.prompt_no_network)
            return
        }

        viewModelScope.launch {
            try {
                val stripeSub = withContext(Dispatchers.IO) {
                    StripeClient.refreshSub(account)
                }

                stripeResult.value = if (stripeSub == null) {
                    Result.LocalizedError(R.string.stripe_refresh_failed)
                } else {
                    Result.Success(stripeSub)
                }

            } catch (e: ClientError) {
                info(e)
                stripeResult.value = if (e.statusCode == 404) {
                    Result.LocalizedError(R.string.loading_failed)
                } else {
                    parseApiError(e)
                }

            } catch (e: Exception) {
                info(e)
                stripeResult.value = parseException(e)
            }
        }
    }

    fun cancelStripe(account: Account) {
        if (isNetworkAvailable.value == false) {
            stripeResult.value = Result.LocalizedError(R.string.prompt_no_network)
            return
        }

        viewModelScope.launch {
            try {
                val stripeSub = withContext(Dispatchers.IO) {
                    StripeClient.cancelSub(account)
                }

                stripeResult.value = if (stripeSub == null) {
                    Result.LocalizedError(R.string.stripe_refresh_failed)
                } else {
                    Result.Success(stripeSub)
                }

            } catch (e: ClientError) {
                info(e)
                stripeResult.value = if (e.statusCode == 404) {
                    Result.LocalizedError(R.string.loading_failed)
                } else {
                    parseApiError(e)
                }

            } catch (e: Exception) {
                info(e)
                stripeResult.value = parseException(e)
            }
        }
    }

    fun reactivateStripe(account: Account) {
        if (isNetworkAvailable.value == false) {
            stripeResult.value = Result.LocalizedError(R.string.prompt_no_network)
            return
        }

        viewModelScope.launch {
            try {
                val stripeSub = withContext(Dispatchers.IO) {
                    StripeClient.reactivateSub(account)
                }

                stripeResult.value = if (stripeSub == null) {
                    Result.LocalizedError(R.string.stripe_refresh_failed)
                } else {
                    Result.Success(stripeSub)
                }

            } catch (e: ClientError) {
                info(e)
                stripeResult.value = if (e.statusCode == 404) {
                    Result.LocalizedError(R.string.loading_failed)
                } else {
                    parseApiError(e)
                }

            } catch (e: Exception) {
                info(e)
                stripeResult.value = parseException(e)
            }
        }
    }

    fun refreshIAPSub(account: Account) {
        if (isNetworkAvailable.value == false) {
            iapRefreshResult.value = Result.LocalizedError(R.string.prompt_no_network)
            return
        }

        viewModelScope.launch {
            try {
                val iapSubs = withContext(Dispatchers.IO) {
                    SubRepo.refreshIAP(account)
                }

                iapRefreshResult.value = if (iapSubs == null) {
                    Result.LocalizedError(R.string.iap_refresh_failed)
                } else {
                    Result.Success(iapSubs)
                }

            } catch (e: ClientError) {
                iapRefreshResult.value =  if (e.statusCode == 404) {
                    Result.LocalizedError(R.string.loading_failed)
                } else {
                    parseApiError(e)
                }

            } catch (e: Exception) {
                iapRefreshResult.value = parseException(e)
            }
        }
    }

    fun switchUI(m: LoginMethod) {
        uiType.value = m
    }

    // Show re-authorzation dialog for wechat.
    fun showReAuth() {
        wxRefreshResult.value = Result.Success(WxRefreshState.ReAuth)
    }

    fun fetchOrders(account: Account) {
        if (isNetworkAvailable.value == false) {
            ordersResult.value = Result.LocalizedError(R.string.prompt_no_network)
            return
        }

        viewModelScope.launch {
            try {
                val orders = withContext(Dispatchers.IO) {
                    SubRepo.listOrders(account)
                }

                ordersResult.value = Result.Success(orders)
            } catch (e: Exception) {
                ordersResult.value = parseException(e)
            }
        }
    }
}
