package com.ft.ftchinese.ui.stripewallet

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.stripesubs.PaymentSheetParams
import com.ft.ftchinese.model.stripesubs.StripePaymentMethod
import com.ft.ftchinese.repository.StripeClient
import com.ft.ftchinese.ui.components.ToastMessage
import com.ft.ftchinese.viewmodel.StripeViewModel
import com.stripe.android.ApiResultCallback
import com.stripe.android.model.SetupIntent
import kotlinx.coroutines.launch

private const val TAG = "StripeWalletViewModel"

class StripeWalletViewModel(application: Application) : StripeViewModel(application) {

    // Cache a setup intent so that we can reuse it if
    // not being confirmed yet.
    private var paymentSheetParams: PaymentSheetParams? = null
    val setupLiveData: MutableLiveData<PaymentSheetParams> by lazy {
        MutableLiveData<PaymentSheetParams>()
    }

    // If a setup intent is created but not confirmed yet,
    // reuse it.
    fun showPaymentSheet(account: Account) {
        if (paymentSheetParams != null) {
            setupLiveData.value = paymentSheetParams
            return
        }

        createSetupIntent(account)
    }

    // Make sure if a setup payment is confirmed, it won't be reused.
    fun clearPaymentSheet() {
        paymentSheetParams = null
    }

    private fun createSetupIntent(account: Account) {
        Log.i(TAG, "Creating setup intent")

        val customerId = account.stripeId ?: return

        if (!ensureNetwork()) {
            return
        }

        progressLiveData.value = true

        viewModelScope.launch {

            val result = StripeClient.asyncSetupWithEphemeral(
                customerId
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
                    paymentSheetParams = result.data
                    showPaymentSheet(account)
                }
            }
        }
    }

    // Callback of Stripe#retrieveSetupIntent.
    val onSetupIntentRetrieved = object : ApiResultCallback<SetupIntent> {
        override fun onError(e: Exception) {
            progressLiveData.value = false
            toastLiveData.value = ToastMessage.fromException(e)
        }

        override fun onSuccess(result: SetupIntent) {
            Log.i(TAG, "Setup intent retrieved $result")
            Log.i(TAG, "Payment method ${result.paymentMethod}, id ${result.paymentMethodId}")

            loadPaymentMethod(result)
        }
    }

    private fun loadPaymentMethod(setupIntent: SetupIntent) {
        val rawPm = setupIntent.paymentMethod
        val pmId = setupIntent.paymentMethodId
        // Here paymentMethod field is null,
        // paymentMethodId is populated.
        if (rawPm != null) {
            paymentMethodSelected.value = StripePaymentMethod.newInstance(rawPm)
            progressLiveData.value = false
            return
        }

        if (pmId != null) {
            viewModelScope.launch {
                val result = StripeClient.asyncRetrievePaymentMethod(pmId)
                progressLiveData.value = false
                when (result) {
                    is FetchResult.LocalizedError -> {
                        toastLiveData.value = ToastMessage.Resource(result.msgId)
                    }
                    is FetchResult.TextError -> {
                        toastLiveData.value = ToastMessage.Text(result.text)
                    }
                    is FetchResult.Success -> {
                        paymentMethodSelected.value = result.data
                    }
                }
            }
        }

        progressLiveData.value = false
    }

    // Set default payment method to customer.
    fun setDefaultPaymentMethod(account: Account, paymentMethod: StripePaymentMethod) {
        if (!ensureNetwork()) {
            return
        }

        val pmId = paymentMethod.id
        if (pmId.isBlank()) {
            toastLiveData.value = ToastMessage.Resource(R.string.stripe_no_payment_selected)
            return
        }

        progressLiveData.value = true
        viewModelScope.launch {

            val result = StripeClient.asyncSetDefaultPaymentMethod(
                account = account,
                pmId = pmId,
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
                    defaultPaymentMethod.value = paymentMethod
                }
            }
        }
    }
}
