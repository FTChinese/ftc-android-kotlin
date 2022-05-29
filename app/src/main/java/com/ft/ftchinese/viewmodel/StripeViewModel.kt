package com.ft.ftchinese.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.stripesubs.StripeCustomer
import com.ft.ftchinese.model.stripesubs.StripePaymentMethod
import com.ft.ftchinese.repository.StripeClient
import com.ft.ftchinese.ui.base.ToastMessage
import kotlinx.coroutines.launch

private const val TAG = "StripeSetupViewModel"

data class PaymentMethodInUse(
    val current: StripePaymentMethod?,
    val isDefault: Boolean,
)

open class StripeViewModel(application: Application) : BaseAppViewModel(application) {

    val customerLiveData : MutableLiveData<StripeCustomer> by lazy {
        MutableLiveData<StripeCustomer>()
    }

    val defaultPaymentMethod: MutableLiveData<StripePaymentMethod> by lazy {
        MutableLiveData<StripePaymentMethod>()
    }

    val paymentMethodSelected: MutableLiveData<StripePaymentMethod> by lazy {
        MutableLiveData<StripePaymentMethod>()
    }

    val paymentMethodInUse = MediatorLiveData<PaymentMethodInUse>().apply {
        addSource(paymentMethodSelected) {
            value = updatePaymentInUse()
        }
        addSource(defaultPaymentMethod) {
            value = updatePaymentInUse()
        }
    }

    private fun updatePaymentInUse(): PaymentMethodInUse {
        if (paymentMethodSelected.value != null) {
            return PaymentMethodInUse(
                current = paymentMethodSelected.value,
                isDefault = paymentMethodSelected.value?.id == defaultPaymentMethod.value?.id
            )
        }

        return PaymentMethodInUse(
            current = defaultPaymentMethod.value,
            isDefault = true,
        )
    }

    fun createCustomer(account: Account) {
        if (!ensureNetwork()) {
            return
        }

        progressLiveData.value = true

        viewModelScope.launch {

            val result = StripeClient.asyncCreateCustomer(account)
            progressLiveData.value = false

            when (result) {
                is FetchResult.LocalizedError -> {
                    toastLiveData.value = ToastMessage.Resource(result.msgId)
                }
                is FetchResult.TextError -> {
                    toastLiveData.value = ToastMessage.Text(result.text)
                }
                is FetchResult.Success -> {
                    customerLiveData.value = result.data
                }
            }
        }
    }

    fun loadDefaultPaymentMethod(account: Account) {
        if (!ensureNetwork()) {
            return
        }

        val cusId = account.stripeId
        if (cusId.isNullOrBlank()) {
            Log.i(TAG, "Not a stripe customer")
            return
        }

        progressLiveData.value = true
        viewModelScope.launch {

            val result = StripeClient.asyncLoadDefaultPaymentMethod(
                cusId = cusId,
                subsId = account.membership.stripeSubsId,
                ftcId = account.id
            )
            progressLiveData.value = false

            when (result) {
                is FetchResult.LocalizedError -> {
                    toastLiveData.value = ToastMessage.Resource(result.msgId)
                }
                is FetchResult.TextError -> {
                    toastLiveData.value = ToastMessage.Text(result.text)
                }
                is FetchResult.Success -> {
                    defaultPaymentMethod.value = result.data
                }
            }
        }
    }
}
