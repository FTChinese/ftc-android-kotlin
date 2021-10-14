package com.ft.ftchinese.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.stripesubs.StripeSubsResult
import com.ft.ftchinese.model.reader.*
import com.ft.ftchinese.model.ftcsubs.Order
import com.ft.ftchinese.model.fetch.APIError
import com.ft.ftchinese.model.iapsubs.IAPSubsResult
import com.ft.ftchinese.repository.AppleClient
import com.ft.ftchinese.repository.StripeClient
import com.ft.ftchinese.repository.FtcPayClient
import com.ft.ftchinese.ui.base.BaseViewModel
import com.ft.ftchinese.ui.data.ApiRequest
import com.ft.ftchinese.model.fetch.FetchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class AccountViewModel : BaseViewModel(), AnkoLogger {

    val uiSwitched: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    val accountRefreshed: MutableLiveData<FetchResult<Account>> by lazy {
        MutableLiveData<FetchResult<Account>>()
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

    // Refresh a user's account data, regardless of logged in
    // via email or wecaht.
    fun refresh(account: Account, manual: Boolean = false) {
        if (isNetworkAvailable.value == false) {
            accountRefreshed.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        if (!manual) {
            progressLiveData.value = true
        }
        viewModelScope.launch {
            info("Start refreshing account")

            accountRefreshed.value = ApiRequest
                .asyncRefreshAccount(account)

            progressLiveData.value = false
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
                    FtcPayClient.useAddOn(account)
                }

                addOnResult.value = if (m == null) {
                    FetchResult.LocalizedError(R.string.loading_failed)
                } else {
                    FetchResult.Success(m)
                }
            } catch (e: APIError) {
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

            } catch (e: APIError) {
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

            } catch (e: APIError) {
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

            } catch (e: APIError) {
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

            } catch (e: APIError) {
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

    fun fetchOrders(account: Account) {
        if (isNetworkAvailable.value == false) {
            ordersResult.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        viewModelScope.launch {
            try {
                val orders = withContext(Dispatchers.IO) {
                    FtcPayClient.listOrders(account)
                }

                ordersResult.value = FetchResult.Success(orders)
            } catch (e: Exception) {
                ordersResult.value = FetchResult.fromException(e)
            }
        }
    }
}
