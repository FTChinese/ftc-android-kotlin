package com.ft.ftchinese.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.stripesubs.StripeSubsResult
import com.ft.ftchinese.model.reader.*
import com.ft.ftchinese.model.ftcsubs.Order
import com.ft.ftchinese.repository.AccountRepo
import com.ft.ftchinese.model.fetch.ClientError
import com.ft.ftchinese.model.iapsubs.IAPSubsResult
import com.ft.ftchinese.repository.AppleClient
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

    val addOnResult: MutableLiveData<Result<Membership>> by lazy {
        MutableLiveData<Result<Membership>>()
    }

    val stripeResult: MutableLiveData<Result<StripeSubsResult>> by lazy {
        MutableLiveData<Result<StripeSubsResult>>()
    }

    val iapRefreshResult: MutableLiveData<Result<IAPSubsResult>> by lazy {
        MutableLiveData<Result<IAPSubsResult>>()
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

    fun migrateAddOn(account: Account) {
        if (isNetworkAvailable.value == false) {
            addOnResult.value = Result.LocalizedError(R.string.prompt_no_network)
            return
        }

        viewModelScope.launch {
            try {
                val m = withContext(Dispatchers.IO) {
                    SubRepo.useAddOn(account)
                }

                addOnResult.value = if (m == null) {
                    Result.LocalizedError(R.string.loading_failed)
                } else {
                    Result.Success(m)
                }
            } catch (e: ClientError) {
                addOnResult.value =  if (e.statusCode == 404) {
                    Result.LocalizedError(R.string.loading_failed)
                } else {
                    parseApiError(e)
                }
            } catch (e: Exception) {
                addOnResult.value = parseException(e)
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

    fun refreshIAP(account: Account) {
        if (isNetworkAvailable.value == false) {
            iapRefreshResult.value = Result.LocalizedError(R.string.prompt_no_network)
            return
        }

        viewModelScope.launch {
            try {
                val iapSubs = withContext(Dispatchers.IO) {
                    AppleClient.refreshIAP(account)
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
