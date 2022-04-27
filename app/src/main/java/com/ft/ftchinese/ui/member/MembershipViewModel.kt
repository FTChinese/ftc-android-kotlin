package com.ft.ftchinese.ui.member

import android.app.Application
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.fetch.APIError
import com.ft.ftchinese.model.iapsubs.IAPSubsResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.stripesubs.StripeSubsResult
import com.ft.ftchinese.repository.AppleClient
import com.ft.ftchinese.repository.FtcPayClient
import com.ft.ftchinese.repository.StripeClient
import com.ft.ftchinese.ui.components.ToastMessage
import com.ft.ftchinese.viewmodel.UserViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "MemberStatusViewModel"

class MembershipViewModel(application: Application): UserViewModel(application) {

    fun refresh() {
        val a = account ?: return
        refreshingLiveData.value = true

        if (a.membership.autoRenewOffExpired && a.membership.hasAddOn) {
            migrateAddOn(a)
            return
        }

        when (a.membership.payMethod) {
            PayMethod.ALIPAY,
            PayMethod.WXPAY,
            PayMethod.B2B -> {
                refreshAccount()
            }
            PayMethod.STRIPE -> {
                refreshStripe(a)
            }
            PayMethod.APPLE -> {
                refreshIAP(a)
            }
            else -> {
                toastLiveData.value = ToastMessage.Text("Current payment method unknown!")
            }
        }
    }

    private fun migrateAddOn(a: Account) {
        if (connectionLiveData.value != true) {
            toastLiveData.value = ToastMessage.Resource(R.string.prompt_no_network)
            return
        }

        toastLiveData.value = ToastMessage.Resource(R.string.refreshing_account)

        viewModelScope.launch {
            try {
                val m = withContext(Dispatchers.IO) {
                    FtcPayClient.useAddOn(a)
                }

                if (m == null) {
                    toastLiveData.value = ToastMessage.Resource(R.string.loading_failed)
                } else {
                    toastLiveData.value = ToastMessage.Resource(R.string.refresh_success)
                    saveMembership(m)
                }
            } catch (e: APIError) {
                toastLiveData.value =  if (e.statusCode == 404) {
                    ToastMessage.Resource(R.string.loading_failed)
                } else {
                    ToastMessage.fromApi(e)
                }
            } catch (e: Exception) {
                toastLiveData.value = ToastMessage.fromException(e)
            }
            refreshingLiveData.value = false
        }
    }

    // Ask the latest stripe subscription data.
    private fun refreshStripe(a: Account) {
        if (connectionLiveData.value != false) {
            toastLiveData.value = ToastMessage.Resource(R.string.prompt_no_network)
            return
        }

        toastLiveData.value = ToastMessage.Resource(R.string.stripe_refreshing)
        viewModelScope.launch {
            try {
                val stripeSub = withContext(Dispatchers.IO) {
                    StripeClient.refreshSub(a)
                }

                if (stripeSub == null) {
                    toastLiveData.value = ToastMessage.Resource(R.string.stripe_refresh_failed)
                } else {
                    toastLiveData.value = ToastMessage.Resource(R.string.stripe_refresh_success)
                    saveStripeSubs(stripeSub)
                }
            } catch (e: APIError) {
                Log.i(TAG, "$e")
                toastLiveData.value = if (e.statusCode == 404) {
                    ToastMessage.Resource(R.string.loading_failed)
                } else {
                    ToastMessage.fromApi(e)
                }

            } catch (e: Exception) {
                Log.i(TAG, "$e")
                toastLiveData.value = ToastMessage.fromException(e)
            }
            refreshingLiveData.value = false
        }
    }

    private suspend fun saveStripeSubs(result: StripeSubsResult) {
        saveMembership(result.membership)
        withContext(Dispatchers.IO) {
            session.saveStripeSubs(result.subs)
        }
    }

    private fun refreshIAP(a: Account) {
        if (connectionLiveData.value == false) {
            toastLiveData.value = ToastMessage.Resource(R.string.prompt_no_network)
            return
        }

        toastLiveData.value = ToastMessage.Resource(R.string.iap_refreshing)

        viewModelScope.launch {
            try {
                val iapSubs = withContext(Dispatchers.IO) {
                    AppleClient.refreshIAP(a)
                }

                if (iapSubs == null) {
                    toastLiveData.value = ToastMessage.Resource(R.string.iap_refresh_failed)
                } else {
                    toastLiveData.value = ToastMessage.Resource(R.string.iap_refresh_success)
                    saveIapSubs(iapSubs)
                }
            } catch (e: APIError) {
                toastLiveData.value =  if (e.statusCode == 404) {
                    ToastMessage.Resource(R.string.loading_failed)
                } else {
                    ToastMessage.fromApi(e)
                }

            } catch (e: Exception) {
                toastLiveData.value = ToastMessage.fromException(e)
            }
            refreshingLiveData.value = false
        }
    }

    private suspend fun saveIapSubs(result: IAPSubsResult) {
        saveMembership(result.membership)
        withContext(Dispatchers.IO) {
            session.saveIapSus(result.subscription)
        }
    }

    fun cancelStripe(account: Account) {
        if (connectionLiveData.value == false) {
            toastLiveData.value = ToastMessage.Resource(R.string.prompt_no_network)
            return
        }

        progressLiveData.value = true
        viewModelScope.launch {
            try {
                val stripeSub = withContext(Dispatchers.IO) {
                    StripeClient.cancelSub(account)
                }

                progressLiveData.value = false
                 if (stripeSub == null) {
                    toastLiveData.value = ToastMessage.Resource(R.string.stripe_refresh_failed)
                } else {
                    toastLiveData.value = ToastMessage.Resource(R.string.stripe_refresh_success)
                    saveStripeSubs(stripeSub)
                }
            } catch (e: APIError) {
                progressLiveData.value = false
                Log.i(TAG, "$e")
                toastLiveData.value = if (e.statusCode == 404) {
                    ToastMessage.Resource(R.string.loading_failed)
                } else {
                    ToastMessage.fromApi(e)
                }

            } catch (e: Exception) {
                progressLiveData.value = false
                Log.i(TAG, "$e")
                toastLiveData.value = ToastMessage.fromException(e)
            }
        }
    }

    fun reactivateStripe(account: Account) {
        if (connectionLiveData.value == false) {
            toastLiveData.value = ToastMessage.Resource(R.string.prompt_no_network)
            return
        }

        toastLiveData.value = ToastMessage.Resource(R.string.stripe_refreshing)
        progressLiveData.value = true

        viewModelScope.launch {
            try {
                val stripeSub = withContext(Dispatchers.IO) {
                    StripeClient.reactivateSub(account)
                }

                progressLiveData.value = false
                if (stripeSub == null) {
                    toastLiveData.value = ToastMessage.Resource(R.string.stripe_refresh_failed)
                } else {
                    toastLiveData.value = ToastMessage.Resource(R.string.stripe_refresh_success)
                    saveStripeSubs(stripeSub)
                }
            } catch (e: APIError) {
                progressLiveData.value = false
                Log.i(TAG, "$e")
                toastLiveData.value = if (e.statusCode == 404) {
                    ToastMessage.Resource(R.string.loading_failed)
                } else {
                    ToastMessage.fromApi(e)
                }

            } catch (e: Exception) {
                progressLiveData.value = false
                Log.i(TAG, "$e")
                toastLiveData.value = ToastMessage.fromException(e)
            }
        }
    }
}
