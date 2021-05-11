package com.ft.ftchinese.ui.customer

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.ClientError
import com.ft.ftchinese.model.fetch.json
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.stripesubs.StripeCustomer
import com.ft.ftchinese.repository.StripeClient
import com.ft.ftchinese.store.CacheFileNames
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.ui.base.BaseViewModel
import com.ft.ftchinese.ui.data.FetchResult
import com.stripe.android.model.PaymentMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class CustomerViewModel(
    private val fileCache: FileCache
) : BaseViewModel(), AnkoLogger {

    // Use when letting user to choose default payment method.
    val customerSessionProgress = MutableLiveData(false)
    val paymentSessionProgress = MutableLiveData(false)
    val bankCardProgress = MediatorLiveData<Boolean>().apply {
        addSource(progressLiveData) {
            value = isSetPaymentInProgress()
        }
        addSource(customerSessionProgress) {
            value = isSetPaymentInProgress()
        }
        addSource(paymentSessionProgress) {
            value = isSetPaymentInProgress()
        }
    }

    private fun isSetPaymentInProgress(): Boolean {
        return progressLiveData.value == true || paymentSessionProgress.value == true || customerSessionProgress.value == true
    }

    val paymentMethodSelected: MutableLiveData<PaymentMethod> by lazy {
        MutableLiveData<PaymentMethod>()
    }

    val isFormEnabled = MediatorLiveData<Boolean>().apply {
        addSource()
    }

    val customerCreated: MutableLiveData<FetchResult<StripeCustomer>> by lazy {
        MutableLiveData<FetchResult<StripeCustomer>>()
    }

    val customerRetrieved: MutableLiveData<FetchResult<StripeCustomer>> by lazy {
        MutableLiveData<FetchResult<StripeCustomer>>()
    }

    val paymentMethodSet: MutableLiveData<FetchResult<StripeCustomer>> by lazy {
        MutableLiveData<FetchResult<StripeCustomer>>()
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
                // Why do I cache this?
                withContext(Dispatchers.IO) {
                    fileCache.saveText(CacheFileNames.stripeCustomer, result.raw)
                }

            } catch (e: ClientError) {

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

    // Load stripe customer.
    fun loadCustomer(account: Account) {
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
                customerRetrieved.value = FetchResult.Success(customer)
                return@launch
            }

            if (isNetworkAvailable.value != true) {
                customerRetrieved.value = FetchResult.LocalizedError(R.string.api_network_failure)
                return@launch
            }

            try {
                val result = withContext(Dispatchers.IO) {
                    StripeClient.retrieveCustomer(account)
                }

                if (result == null) {
                    customerRetrieved.value = FetchResult.LocalizedError(R.string.stripe_customer_not_found)
                    return@launch
                }

                customerRetrieved.value = FetchResult.Success(result.value)

                withContext(Dispatchers.IO) {
                    fileCache.saveText(CacheFileNames.stripeCustomer, result.raw)
                }
            } catch (e: Exception) {
                customerRetrieved.value = FetchResult.fromException(e)
            }
        }
    }

    // Set default payment method.
    fun setDefaultPaymentMethod(account: Account, pmId: String) {
        if (isNetworkAvailable.value == false) {
            paymentMethodSet.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        progressLiveData.value = true
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    StripeClient.setDefaultPaymentMethod(account, pmId)
                }

                if (result == null) {
                    paymentMethodSet.value = FetchResult.LocalizedError(R.string.stripe_customer_not_found)
                    progressLiveData.value = false
                    return@launch
                }

                paymentMethodSet.value = FetchResult.Success(result.value)
                progressLiveData.value = false

                withContext(Dispatchers.IO) {
                    fileCache.saveText(CacheFileNames.stripeCustomer, result.raw)
                }
            } catch (e: ClientError) {

                progressLiveData.value = false
                paymentMethodSet.value = if (e.statusCode == 404) {
                    FetchResult.LocalizedError(R.string.stripe_customer_not_found)
                } else {
                    FetchResult.fromServerError(e)
                }
            } catch (e: Exception) {
                info(e)
                progressLiveData.value = false
                paymentMethodSet.value = FetchResult.fromException(e)
            }
        }
    }
}
