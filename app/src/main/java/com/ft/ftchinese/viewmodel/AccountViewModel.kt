package com.ft.ftchinese.viewmodel

import androidx.lifecycle.MutableLiveData
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
import com.ft.ftchinese.ui.base.BaseViewModel
import com.ft.ftchinese.ui.data.FetchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import java.io.ByteArrayInputStream
import java.io.InputStream

class AccountViewModel : BaseViewModel(), AnkoLogger {

    val inProgress: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    val uiType = MutableLiveData<LoginMethod>()

    val accountRefreshed: MutableLiveData<FetchResult<Account>> by lazy {
        MutableLiveData<FetchResult<Account>>()
    }

    val avatarRetrieved: MutableLiveData<FetchResult<InputStream>> by lazy {
        MutableLiveData<FetchResult<InputStream>>()
    }

    val wxRefreshResult: MutableLiveData<FetchResult<WxRefreshState>> by lazy {
        MutableLiveData<FetchResult<WxRefreshState>>()
    }

    val addOnResult: MutableLiveData<FetchResult<Membership>> by lazy {
        MutableLiveData<FetchResult<Membership>>()
    }

    val stripeResult: MutableLiveData<FetchResult<StripeSubsResult>> by lazy {
        MutableLiveData<FetchResult<StripeSubsResult>>()
    }

    val iapRefreshResult: MutableLiveData<FetchResult<IAPSubsResult>> by lazy {
        MutableLiveData<FetchResult<IAPSubsResult>>()
    }

    val ordersResult: MutableLiveData<FetchResult<List<Order>>> by lazy {
        MutableLiveData<FetchResult<List<Order>>>()
    }

    // Ask API to fetch user's latest wechat info and save
    // it to database.
    fun refreshWxInfo(wxSession: WxSession) {
        if (isNetworkAvailable.value == false) {
            wxRefreshResult.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        viewModelScope.launch {
            try {
                val done = withContext(Dispatchers.IO) {
                    AccountRepo.refreshWxInfo(wxSession)
                }

                info("Refresh wx info result: $done")
                wxRefreshResult.value = if (done) {
                    FetchResult.Success(WxRefreshState.SUCCESS)
                } else {
                    FetchResult.Success(WxRefreshState.ReAuth)
                }

            } catch (e: ClientError) {
                info("Refresh wx info api error: $e")
                wxRefreshResult.value = when (e.statusCode) {
                    422 -> FetchResult.Success(WxRefreshState.ReAuth)
                    404 -> FetchResult.LocalizedError(R.string.api_account_not_found)
                    else -> FetchResult.fromServerError(e)
                }

            } catch (e: Exception) {
                info("Refresh wx info exception: $e")
                wxRefreshResult.value = FetchResult.fromException(e)
            }
        }
    }

    // Refresh a user's account data, regardless of logged in
    // via email or wecaht.
    fun refresh(account: Account) {
        if (isNetworkAvailable.value == false) {
            accountRefreshed.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        // TODO: progress indicator
        viewModelScope.launch {
            info("Start refreshing account")

            try {

                val updatedAccount = withContext(Dispatchers.IO) {
                    AccountRepo.refresh(account)
                }

                if (updatedAccount == null) {
                    info("Refresh account returned empty data")
                    accountRefreshed.value = FetchResult.LocalizedError(R.string.loading_failed)
                    return@launch
                }

                info("Refresh account success")
                accountRefreshed.value = FetchResult.Success(updatedAccount)
            } catch (e: ClientError) {

                info("Refresh account api error $e")

                accountRefreshed.value = if (e.statusCode == 404) {
                    FetchResult.LocalizedError(R.string.api_account_not_found)
                } else {
                    FetchResult.fromServerError(e)
                }

            } catch (e: Exception) {

                info("Refresh account exception $e")

                accountRefreshed.value = FetchResult.fromException(e)
            }
        }
    }

    fun fetchWxAvatar(cache: FileCache, wechat: Wechat) {
        if (wechat.avatarUrl == null) {
            info("Wx avatar url empty")
            return
        }

        if (isNetworkAvailable.value == false) {
            avatarRetrieved.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        viewModelScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    AccountRepo.loadWxAvatar(wechat.avatarUrl)
                } ?: return@launch

                avatarRetrieved.value = FetchResult.Success(ByteArrayInputStream(bytes))

                withContext(Dispatchers.IO) {
                    cache.writeBinaryFile(
                            WX_AVATAR_NAME,
                            bytes
                    )
                }

            } catch (e: Exception) {

                avatarRetrieved.value = FetchResult.fromException(e)
            }
        }
    }

    fun migrateAddOn(account: Account) {
        if (isNetworkAvailable.value == false) {
            addOnResult.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        viewModelScope.launch {
            try {
                val m = withContext(Dispatchers.IO) {
                    SubRepo.useAddOn(account)
                }

                addOnResult.value = if (m == null) {
                    FetchResult.LocalizedError(R.string.loading_failed)
                } else {
                    FetchResult.Success(m)
                }
            } catch (e: ClientError) {
                addOnResult.value =  if (e.statusCode == 404) {
                    FetchResult.LocalizedError(R.string.loading_failed)
                } else {
                    FetchResult.fromServerError(e)
                }
            } catch (e: Exception) {
                addOnResult.value = FetchResult.fromException(e)
            }
        }
    }

    // Ask the latest stripe subscription data.
    fun refreshStripe(account: Account) {
        if (isNetworkAvailable.value == false) {
            stripeResult.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        viewModelScope.launch {
            try {
                val stripeSub = withContext(Dispatchers.IO) {
                    StripeClient.refreshSub(account)
                }

                stripeResult.value = if (stripeSub == null) {
                    FetchResult.LocalizedError(R.string.stripe_refresh_failed)
                } else {
                    FetchResult.Success(stripeSub)
                }

            } catch (e: ClientError) {
                info(e)
                stripeResult.value = if (e.statusCode == 404) {
                    FetchResult.LocalizedError(R.string.loading_failed)
                } else {
                    FetchResult.fromServerError(e)
                }

            } catch (e: Exception) {
                info(e)
                stripeResult.value = FetchResult.fromException(e)
            }
        }
    }

    fun cancelStripe(account: Account) {
        if (isNetworkAvailable.value == false) {
            stripeResult.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        viewModelScope.launch {
            try {
                val stripeSub = withContext(Dispatchers.IO) {
                    StripeClient.cancelSub(account)
                }

                stripeResult.value = if (stripeSub == null) {
                    FetchResult.LocalizedError(R.string.stripe_refresh_failed)
                } else {
                    FetchResult.Success(stripeSub)
                }

            } catch (e: ClientError) {
                info(e)
                stripeResult.value = if (e.statusCode == 404) {
                    FetchResult.LocalizedError(R.string.loading_failed)
                } else {
                    FetchResult.fromServerError(e)
                }

            } catch (e: Exception) {
                info(e)
                stripeResult.value = FetchResult.fromException(e)
            }
        }
    }

    fun reactivateStripe(account: Account) {
        if (isNetworkAvailable.value == false) {
            stripeResult.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        viewModelScope.launch {
            try {
                val stripeSub = withContext(Dispatchers.IO) {
                    StripeClient.reactivateSub(account)
                }

                stripeResult.value = if (stripeSub == null) {
                    FetchResult.LocalizedError(R.string.stripe_refresh_failed)
                } else {
                    FetchResult.Success(stripeSub)
                }

            } catch (e: ClientError) {
                info(e)
                stripeResult.value = if (e.statusCode == 404) {
                    FetchResult.LocalizedError(R.string.loading_failed)
                } else {
                    FetchResult.fromServerError(e)
                }

            } catch (e: Exception) {
                info(e)
                stripeResult.value = FetchResult.fromException(e)
            }
        }
    }

    fun refreshIAP(account: Account) {
        if (isNetworkAvailable.value == false) {
            iapRefreshResult.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        viewModelScope.launch {
            try {
                val iapSubs = withContext(Dispatchers.IO) {
                    AppleClient.refreshIAP(account)
                }

                iapRefreshResult.value = if (iapSubs == null) {
                    FetchResult.LocalizedError(R.string.iap_refresh_failed)
                } else {
                    FetchResult.Success(iapSubs)
                }

            } catch (e: ClientError) {
                iapRefreshResult.value =  if (e.statusCode == 404) {
                    FetchResult.LocalizedError(R.string.loading_failed)
                } else {
                    FetchResult.fromServerError(e)
                }

            } catch (e: Exception) {
                iapRefreshResult.value = FetchResult.fromException(e)
            }
        }
    }

    fun switchUI(m: LoginMethod) {
        uiType.value = m
    }

    // Show re-authorzation dialog for wechat.
    fun showReAuth() {
        wxRefreshResult.value = FetchResult.Success(WxRefreshState.ReAuth)
    }

    fun fetchOrders(account: Account) {
        if (isNetworkAvailable.value == false) {
            ordersResult.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        viewModelScope.launch {
            try {
                val orders = withContext(Dispatchers.IO) {
                    SubRepo.listOrders(account)
                }

                ordersResult.value = FetchResult.Success(orders)
            } catch (e: Exception) {
                ordersResult.value = FetchResult.fromException(e)
            }
        }
    }
}
