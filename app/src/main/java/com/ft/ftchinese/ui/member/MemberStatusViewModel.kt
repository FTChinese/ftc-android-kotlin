package com.ft.ftchinese.ui.member

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.APIError
import com.ft.ftchinese.model.iapsubs.IAPSubsResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Membership
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

class MemberStatusViewModel(application: Application): UserViewModel(application) {

    val addonLiveData: MutableLiveData<Membership> by lazy {
        MutableLiveData<Membership>()
    }

    val stripeSubsLiveData: MutableLiveData<StripeSubsResult> by lazy {
        MutableLiveData<StripeSubsResult>()
    }

    val iapSubsLiveData: MutableLiveData<IAPSubsResult> by lazy {
        MutableLiveData<IAPSubsResult>()
    }

    fun migrateAddOn(account: Account) {
        if (connectionLiveData.value != true) {
            toastLiveData.value = ToastMessage.Resource(R.string.prompt_no_network)
            return
        }

        viewModelScope.launch {
            try {
                val m = withContext(Dispatchers.IO) {
                    FtcPayClient.useAddOn(account)
                }

                if (m == null) {
                    toastLiveData.value = ToastMessage.Resource(R.string.loading_failed)
                } else {
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
        }
    }

    // Ask the latest stripe subscription data.
    fun refreshStripe(account: Account) {
        if (connectionLiveData.value != false) {
            toastLiveData.value = ToastMessage.Resource(R.string.prompt_no_network)
            return
        }

        viewModelScope.launch {
            try {
                val stripeSub = withContext(Dispatchers.IO) {
                    StripeClient.refreshSub(account)
                }

                if (stripeSub == null) {
                    toastLiveData.value = ToastMessage.Resource(R.string.stripe_refresh_failed)
                } else {
                    stripeSubsLiveData.value = stripeSub
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
        }
    }

    fun cancelStripe(account: Account) {
        if (connectionLiveData.value == false) {
            toastLiveData.value = ToastMessage.Resource(R.string.prompt_no_network)
            return
        }

        viewModelScope.launch {
            try {
                val stripeSub = withContext(Dispatchers.IO) {
                    StripeClient.cancelSub(account)
                }

                 if (stripeSub == null) {
                    toastLiveData.value = ToastMessage.Resource(R.string.stripe_refresh_failed)
                } else {
                    stripeSubsLiveData.value = stripeSub
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
        }
    }

    fun reactivateStripe(account: Account) {
        if (connectionLiveData.value == false) {
            toastLiveData.value = ToastMessage.Resource(R.string.prompt_no_network)
            return
        }

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
                    stripeSubsLiveData.value = stripeSub
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

    fun refreshIAP(account: Account) {
        if (connectionLiveData.value == false) {
            toastLiveData.value = ToastMessage.Resource(R.string.prompt_no_network)
            return
        }

        viewModelScope.launch {
            try {
                val iapSubs = withContext(Dispatchers.IO) {
                    AppleClient.refreshIAP(account)
                }

                if (iapSubs == null) {
                    toastLiveData.value = ToastMessage.Resource(R.string.iap_refresh_failed)
                } else {
                    iapSubsLiveData.value = iapSubs
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
        }
    }
}
