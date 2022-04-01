package com.ft.ftchinese.ui.customer

import android.util.Log
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.FetchUi
import com.ft.ftchinese.model.fetch.APIError
import com.ft.ftchinese.model.fetch.json
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.stripesubs.StripeCustomer
import com.ft.ftchinese.repository.StripeClient
import com.ft.ftchinese.store.CacheFileNames
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.ui.base.BaseViewModel
import com.ft.ftchinese.model.fetch.FetchResult
import com.stripe.android.model.PaymentMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CustomerViewModel(
    private val fileCache: FileCache
) : BaseViewModel() {

    // Determines whether payment method control should be enabled.
    val customerLoaded: MutableLiveData<StripeCustomer> by lazy {
        MutableLiveData<StripeCustomer>()
    }

    val errorLiveData: MutableLiveData<FetchUi> by lazy {
        MutableLiveData<FetchUi>()
    }

    // When a customer is created.
    val customerCreated: MutableLiveData<FetchResult<StripeCustomer>> by lazy {
        MutableLiveData<FetchResult<StripeCustomer>>()
    }

    // When the default payment method is set.
    val paymentMethodUpdated: MutableLiveData<FetchResult<StripeCustomer>> by lazy {
        MutableLiveData<FetchResult<StripeCustomer>>()
    }

    // Use when letting user to choose default payment method.
    val customerSessionProgress = MutableLiveData(false)

    val progressMediatorLiveData = MediatorLiveData<Boolean>().apply {
        addSource(progressLiveData) {
            value = it == true || customerSessionProgress.value == true
        }
        addSource(customerSessionProgress) {
            value = it == true || progressLiveData.value == true
        }
    }

    // Store the value if a payment method is selected.
    val paymentMethodSelected: MutableLiveData<PaymentMethod> by lazy {
        MutableLiveData<PaymentMethod>()
    }

    // Enable/Disable button
    val isFormEnabled = MediatorLiveData<Boolean>().apply {
        value = false
        addSource(progressLiveData) {
            value = enableForm()
        }
        addSource(customerSessionProgress) {

        }
        addSource(customerLoaded) {
            value = enableForm()
        }
        addSource(paymentMethodSelected) {
            value = enableForm()
        }
    }

    private fun enableForm(): Boolean {
        if (progressLiveData.value == true) {
            return false
        }

        if (customerSessionProgress.value == true) {
            return false
        }

        if (paymentMethodSelected.value == null) {
            return false
        }

        // Payment method selected or retrieved.
        if (customerLoaded.value == null) {
            return false
        }

        return customerLoaded.value?.defaultPaymentMethod != paymentMethodSelected.value?.id
    }

    // Show/hide default icon.
    val defaultIconVisible = MediatorLiveData<Boolean>().apply {

        value = false
        addSource(customerLoaded) {
            value = showDefaultIcon()
        }
        addSource(paymentMethodSelected) {
            value = showDefaultIcon()
        }
    }

    private fun showDefaultIcon(): Boolean {
        if (paymentMethodSelected.value == null) {
            return false
        }

        // Payment method selected or retrieved.
        if (customerLoaded.value == null) {
            return false
        }

        return customerLoaded.value?.defaultPaymentMethod == paymentMethodSelected.value?.id
    }

    // Load stripe customer.
    fun loadCustomer(account: Account) {
        if (isNetworkAvailable.value == false) {
            errorLiveData.value = FetchUi.ResMsg(R.string.prompt_no_network)
            return
        }

        // Load from cache first.
        progressLiveData.value = true
        viewModelScope.launch {
            val customer = withContext(Dispatchers.IO) {
                val data = fileCache.loadText(CacheFileNames.stripeCustomer)

                if (data == null) {
                    null
                } else {
                    try {
                        json.parse<StripeCustomer>(data)
                    } catch (e: Exception) {
                        null
                    }
                }
            }

            if (customer != null) {
                customerLoaded.value = customer
                progressLiveData.value = false
                return@launch
            }

            if (isNetworkAvailable.value != true) {
                errorLiveData.value = FetchUi.ResMsg(R.string.api_network_failure)
                progressLiveData.value = false
                return@launch
            }

            try {
                val resp = withContext(Dispatchers.IO) {
                    StripeClient.retrieveCustomer(account)
                }

                progressLiveData.value = false
                if (resp.body == null) {
                    errorLiveData.value = FetchUi.ResMsg(R.string.stripe_customer_not_found)
                    return@launch
                }

                customerLoaded.value = resp.body

                cacheStripeCustomer(resp.raw)
            } catch (e: Exception) {
                progressLiveData.value = false
                errorLiveData.value = FetchUi.fromException(e)
            }
        }
    }

    // Create stripe customer.
    fun createCustomer(account: Account) {
        if (isNetworkAvailable.value == false) {
            customerCreated.value = FetchResult.LocalizedError(R.string.prompt_no_network)
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
                    customerCreated.value = FetchResult.LocalizedError(R.string.stripe_customer_not_created)
                    return@launch
                }

                customerCreated.value = FetchResult.Success(resp.body)
                cacheStripeCustomer(resp.raw)
            } catch (e: APIError) {

                progressLiveData.value = false
                customerCreated.value = if (e.statusCode == 404) {
                    FetchResult.LocalizedError(R.string.stripe_customer_not_found)
                } else {
                    FetchResult.fromApi(e)
                }
            } catch (e: Exception) {
                progressLiveData.value = false
                customerCreated.value = FetchResult.fromException(e)
            }
        }
    }

    // Set default payment method.
    fun setDefaultPaymentMethod(account: Account) {
        if (isNetworkAvailable.value == false) {
            paymentMethodUpdated.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        val pmId = paymentMethodSelected.value?.id
        if (pmId.isNullOrBlank()) {
            paymentMethodUpdated.value = FetchResult.LocalizedError(R.string.stripe_no_payment_selected)
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
                    paymentMethodUpdated.value = FetchResult.LocalizedError(R.string.stripe_customer_not_found)
                    return@launch
                }

                paymentMethodUpdated.value = FetchResult.Success(resp.body)
                customerLoaded.value = resp.body
                cacheStripeCustomer(resp.raw)
            } catch (e: APIError) {

                progressLiveData.value = false
                paymentMethodUpdated.value = if (e.statusCode == 404) {
                    FetchResult.LocalizedError(R.string.stripe_customer_not_found)
                } else {
                    FetchResult.fromApi(e)
                }
            } catch (e: Exception) {
                Log.i(TAG, "$e")
                progressLiveData.value = false
                paymentMethodUpdated.value = FetchResult.fromException(e)
            }
        }
    }

    private suspend fun cacheStripeCustomer(rawJson: String) {
        if (rawJson.isEmpty()) {
            return
        }

        withContext(Dispatchers.IO) {
            fileCache.saveText(CacheFileNames.stripeCustomer, rawJson)
        }
    }

    companion object {
        private const val TAG = "CustomerViewModel"
    }
}
