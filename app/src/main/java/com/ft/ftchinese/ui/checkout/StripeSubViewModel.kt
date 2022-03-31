package com.ft.ftchinese.ui.checkout

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.APIError
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.paywall.CartItemStripeV2
import com.ft.ftchinese.model.paywall.IntentKind
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.stripesubs.StripeSubsResult
import com.ft.ftchinese.model.stripesubs.SubParams
import com.ft.ftchinese.repository.StripeClient
import com.ft.ftchinese.ui.base.BaseViewModel
import com.stripe.android.model.Customer
import com.stripe.android.model.PaymentMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StripeSubViewModel : BaseViewModel() {
    val messageLiveData: MutableLiveData<Int> by lazy {
        MutableLiveData<Int>()
    }

    val itemLiveData: MutableLiveData<CartItemStripeV2> by lazy {
        MutableLiveData<CartItemStripeV2>()
    }

    val customerLiveData: MutableLiveData<Customer> by lazy {
        MutableLiveData<Customer>()
    }

    val isUpdate: Boolean
        get() = itemLiveData.value?.intent?.kind == IntentKind.Upgrade

    val subsResult: MutableLiveData<FetchResult<StripeSubsResult>> by lazy {
        MutableLiveData<FetchResult<StripeSubsResult>>()
    }

    val paymentMethodLiveData: MutableLiveData<PaymentMethod> by lazy {
        MutableLiveData<PaymentMethod>()
    }

    private fun enableSubmit(): Boolean {
        if (progressLiveData.value == true) {
            return false
        }

        if (itemLiveData.value == null) {
            return false
        }

        if (customerLiveData.value == null) {
            return false
        }

        if (paymentMethodLiveData.value == null) {
            return false
        }

        return true
    }

    private fun enablePayMethod(): Boolean {
        if (progressLiveData.value == true) {
            return false
        }

        if (itemLiveData.value == null) {
            return false
        }

        if (customerLiveData.value == null) {
            return false
        }

        return true
    }

    val isPayMethodEnabled = MediatorLiveData<Boolean>().apply {
        addSource(progressLiveData) {
            value = enablePayMethod()
        }
        addSource(itemLiveData) {
            value = enablePayMethod()
        }
        addSource(customerLiveData) {
            value = enablePayMethod()
        }
    }

    val isSubmitEnabled = MediatorLiveData<Boolean>().apply {
        addSource(progressLiveData) {
            value = enableSubmit()
        }
        addSource(itemLiveData) {
            value = enableSubmit()
        }
        addSource(customerLiveData) {
            value = enablePayMethod()
        }
        addSource(paymentMethodLiveData) {
            value = enableSubmit()
        }
    }

    /**
     * When the UI is created, use the price and current
     * membership to build PaymentCounter.
     */
    fun putIntoCart(item: CartItemStripeV2) {
        itemLiveData.value = item
    }

    fun subscribe(account: Account, idemKey: String) {
        if (isNetworkAvailable.value == false) {
            subsResult.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        val item = itemLiveData.value ?: return
        val params = SubParams(
            priceId = item.recurring.id,
            introductoryPriceId = item.trial?.id,
            defaultPaymentMethod = paymentMethodLiveData.value?.id,
            idempotency = idemKey,
        )

        progressLiveData.value = true

        when (item.intent.kind) {
            IntentKind.Create -> {
                messageLiveData.value = R.string.creating_subscription
                createStripeSub(account, params)
            }
            IntentKind.Upgrade, IntentKind.Downgrade -> {
                messageLiveData.value = R.string.confirm_upgrade
                upgradeStripeSub(account, params)
            }
            else -> {
                subsResult.value = FetchResult.LocalizedError(R.string.unknown_order_kind)
                progressLiveData.value = false
            }
        }
    }

    private fun createStripeSub(account: Account, params: SubParams) {
        val pm = paymentMethodLiveData.value
        if (pm == null) {
            subsResult.value = FetchResult.LocalizedError(R.string.toast_no_pay_method)
            return
        }
        if (params.defaultPaymentMethod == null) {
            subsResult.value = FetchResult.LocalizedError(R.string.toast_no_pay_method)
            return
        }

        viewModelScope.launch {
            try {
                val sub = withContext(Dispatchers.IO) {
                    StripeClient.createSubscription(account, params)
                }

                if (sub == null) {
                    subsResult.value = FetchResult.LocalizedError(R.string.error_unknown)
                    return@launch
                }

                subsResult.value = FetchResult.Success(sub)

            } catch (e: APIError) {
                subsResult.value = if (e.type == "idempotency_error") {
                    FetchResult.Error(IdempotencyError())
                } else {
                    FetchResult.fromServerError(e)
                }

            } catch (e: Exception) {
                subsResult.value = FetchResult.fromException(e)
            }
        }
    }

    private fun upgradeStripeSub(account: Account, params: SubParams) {

        viewModelScope.launch {
            try {
                val sub = withContext(Dispatchers.IO) {
                    StripeClient.updateSubs(account, params)
                }

                if (sub == null) {
                    subsResult.value = FetchResult.LocalizedError(R.string.error_unknown)
                    return@launch
                }

                subsResult.value = FetchResult.Success(sub)

            } catch (e: Exception) {
                subsResult.value = FetchResult.fromException(e)
            }
        }
    }
}
