package com.ft.ftchinese.ui.stripewallet

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.APIError
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.stripesubs.PaymentSheetParams
import com.ft.ftchinese.model.stripesubs.StripePaymentMethod
import com.ft.ftchinese.repository.StripeClient
import com.ft.ftchinese.ui.components.ToastMessage
import com.ft.ftchinese.viewmodel.StripeViewModel
import com.stripe.android.ApiResultCallback
import com.stripe.android.model.SetupIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    private suspend fun asyncCreateSetupIntent(customerId: String): Pair<PaymentSheetParams?, ToastMessage?> {
        try {
            val resp = withContext(Dispatchers.IO) {
                StripeClient.setupWithEphemeral(customerId)
            }

            if (resp.body == null) {
                return Pair(null, ToastMessage.errorUnknown)
            }

            return Pair(resp.body, null)
        } catch (e: APIError) {
            return Pair(null, ToastMessage.fromApi(e))
        } catch (e: Exception) {
            return Pair(null, ToastMessage.fromException(e))
        }
    }

    private fun createSetupIntent(account: Account) {
        Log.i(TAG, "Creating setup intent")

        val customerId = account.stripeId ?: return

        if (!ensureNetwork()) {
            return
        }

        progressLiveData.value = true

        viewModelScope.launch {

            val (params, errToast) = asyncCreateSetupIntent(customerId)

            progressLiveData.value = false
            if (errToast != null) {
                toastLiveData.value = errToast
            } else if (params != null) {
                paymentSheetParams = params
                showPaymentSheet(account)
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
                val (pm, errMsg) = asyncLoadPaymentMethod(pmId)

                if (errMsg != null) {
                    toastLiveData.value = errMsg
                    return@launch
                }

                if (pm != null) {
                    paymentMethodSelected.value = pm
                }
            }
        }

        progressLiveData.value = false
    }

    private suspend fun asyncLoadPaymentMethod(id: String): Pair<StripePaymentMethod?, ToastMessage?> {
        return try {
            val resp = withContext(Dispatchers.IO) {
                StripeClient.retrievePaymentMethod(id)
            }

            if (resp.body == null) {
                Pair(null, ToastMessage.errorUnknown)
            } else {
                Pair(resp.body, null)
            }
        } catch (e: APIError) {
            Pair(null, ToastMessage.fromApi(e))
        } catch (e: Exception) {
            Pair(null, ToastMessage.fromException(e))
        }
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
            try {
                val resp = withContext(Dispatchers.IO) {
                    StripeClient.setDefaultPaymentMethod(account, pmId)
                }

                progressLiveData.value = false

                if (resp.body == null) {
                    toastLiveData.value = ToastMessage.Resource(R.string.stripe_customer_not_found)
                    return@launch
                }

                // Disable set default button
                defaultPaymentMethod.value = paymentMethod
            } catch (e: APIError) {
                toastLiveData.value = if (e.statusCode == 404) {
                    ToastMessage.Resource(R.string.stripe_customer_not_found)
                } else {
                    ToastMessage.fromApi(e)
                }
            } catch (e: Exception) {
                Log.i(TAG, "$e")
                toastLiveData.value = ToastMessage.fromException(e)
            } finally {
                progressLiveData.value = false
            }
        }
    }
}
