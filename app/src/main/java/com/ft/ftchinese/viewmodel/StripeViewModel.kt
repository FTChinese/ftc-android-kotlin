package com.ft.ftchinese.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.APIError
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.stripesubs.StripeCustomer
import com.ft.ftchinese.model.stripesubs.StripePaymentMethod
import com.ft.ftchinese.repository.StripeClient
import com.ft.ftchinese.ui.components.ToastMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            try {
                val resp = withContext(Dispatchers.IO) {
                    StripeClient.createCustomer(account)
                }

                progressLiveData.value = false
                if (resp.body == null) {
                    toastLiveData.value = ToastMessage.Resource(R.string.stripe_customer_not_created)
                    return@launch
                }

                customerLiveData.value = resp.body

            } catch (e: APIError) {
                progressLiveData.value = false
                toastLiveData.value = if (e.statusCode == 404) {
                    ToastMessage.Resource(R.string.stripe_customer_not_found)
                } else {
                    ToastMessage.fromApi(e)
                }
            } catch (e: Exception) {
                progressLiveData.value = false
                toastLiveData.value = ToastMessage.fromException(e)
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
            try {
                val resp = withContext(Dispatchers.IO) {
                    StripeClient.loadDefaultPaymentMethod(
                        cusId = cusId,
                        subsId = account.membership.stripeSubsId,
                        ftcId = account.id
                    )
                }

                progressLiveData.value = false
                if (resp.body == null) {
                    return@launch
                }

                defaultPaymentMethod.value = resp.body
            } catch (e: Exception) {
                progressLiveData.value = false
                Log.i(TAG, e.message ?: "")
            } finally {
                progressLiveData.value = false
            }
        }
    }
}
