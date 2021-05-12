package com.ft.ftchinese.ui.customer

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.FetchError
import com.ft.ftchinese.model.fetch.ServerError
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
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class CustomerViewModel(
    private val fileCache: FileCache
) : BaseViewModel(), AnkoLogger {

    val customerLoaded: MutableLiveData<StripeCustomer> by lazy {
        MutableLiveData<StripeCustomer>()
    }

    val errorLiveData: MutableLiveData<FetchError> by lazy {
        MutableLiveData<FetchError>()
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

    // When a customer is created.
    val customerCreated: MutableLiveData<FetchResult<StripeCustomer>> by lazy {
        MutableLiveData<FetchResult<StripeCustomer>>()
    }

    // When the default payment method is set.
    val paymentMethodUpdated: MutableLiveData<FetchResult<StripeCustomer>> by lazy {
        MutableLiveData<FetchResult<StripeCustomer>>()
    }

    // Load stripe customer.
    fun loadCustomer(account: Account) {
        if (isNetworkAvailable.value == false) {
            errorLiveData.value = FetchError.ResourceId(R.string.prompt_no_network)
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
                errorLiveData.value = FetchError.ResourceId(R.string.api_network_failure)
                progressLiveData.value = false
                return@launch
            }

            try {
                val result = withContext(Dispatchers.IO) {
                    StripeClient.retrieveCustomer(account)
                }

                progressLiveData.value = false
                if (result == null) {
                    errorLiveData.value = FetchError.ResourceId(R.string.stripe_customer_not_found)
                    return@launch
                }

                customerLoaded.value = result.value

                withContext(Dispatchers.IO) {
                    fileCache.saveText(CacheFileNames.stripeCustomer, result.raw)
                }
            } catch (e: Exception) {
                progressLiveData.value = false
                errorLiveData.value = FetchError.fromException(e)
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
                val result = withContext(Dispatchers.IO) {
                    StripeClient.createCustomer(account)
                }

                progressLiveData.value = false

                if (result == null) {
                    customerCreated.value = FetchResult.LocalizedError(R.string.stripe_customer_not_created)
                    return@launch
                }

                customerCreated.value = FetchResult.Success(result.value)
                cacheStripeCustomer(result.raw)
            } catch (e: ServerError) {

                progressLiveData.value = false
                customerCreated.value = if (e.statusCode == 404) {
                    FetchResult.LocalizedError(R.string.stripe_customer_not_found)
                } else {
                    FetchResult.fromServerError(e)
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
                val result = withContext(Dispatchers.IO) {
                    StripeClient.setDefaultPaymentMethod(account, pmId)
                }

                progressLiveData.value = false

                if (result == null) {
                    paymentMethodUpdated.value = FetchResult.LocalizedError(R.string.stripe_customer_not_found)
                    return@launch
                }

                paymentMethodUpdated.value = FetchResult.Success(result.value)
                cacheStripeCustomer(result.raw)
            } catch (e: ServerError) {

                progressLiveData.value = false
                paymentMethodUpdated.value = if (e.statusCode == 404) {
                    FetchResult.LocalizedError(R.string.stripe_customer_not_found)
                } else {
                    FetchResult.fromServerError(e)
                }
            } catch (e: Exception) {
                info(e)
                progressLiveData.value = false
                paymentMethodUpdated.value = FetchResult.fromException(e)
            }
        }
    }

    private suspend fun cacheStripeCustomer(rawJson: String) {
        withContext(Dispatchers.IO) {
            fileCache.saveText(CacheFileNames.stripeCustomer, rawJson)
        }
    }
}
