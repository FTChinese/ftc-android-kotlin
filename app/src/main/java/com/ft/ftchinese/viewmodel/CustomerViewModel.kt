package com.ft.ftchinese.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.stripesubs.StripeCustomer
import com.ft.ftchinese.model.fetch.ClientError
import com.ft.ftchinese.repository.StripeClient
import com.ft.ftchinese.store.AccountStore
import com.ft.ftchinese.store.CacheFileNames
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.model.fetch.json
import com.ft.ftchinese.ui.data.FetchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class CustomerViewModel(
    private val fileCache: FileCache
) : ViewModel(), AnkoLogger {

    val isNetworkAvailable = MutableLiveData<Boolean>()

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
    fun create(account: Account) {
        if (isNetworkAvailable.value == false) {
            customerCreated.value = FetchResult.LocalizedError(R.string.prompt_no_network)
            return
        }

        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    StripeClient.createCustomer(account)
                }

                if (result == null) {

                    customerCreated.value = FetchResult.LocalizedError(R.string.stripe_customer_not_created)
                    return@launch
                }

                result.value.let {
                    customerCreated.value = FetchResult.Success(it)
                    AccountStore.customer = it
                }

                withContext(Dispatchers.IO) {
                    fileCache.saveText(CacheFileNames.stripeCustomer, result.raw)
                }

            } catch (e: ClientError) {

                customerCreated.value = if (e.statusCode == 404) {
                    FetchResult.LocalizedError(R.string.stripe_customer_not_found)
                } else {
                    FetchResult.fromServerError(e)
                }
            } catch (e: Exception) {

                customerCreated.value = FetchResult.fromException(e)
            }
        }
    }

    // Load stripe customer.
    fun load(account: Account) {
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
                AccountStore.customer = customer
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
                AccountStore.customer = result.value

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

        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    StripeClient.setDefaultPaymentMethod(account, pmId)
                }

                if (result == null) {
                    paymentMethodSet.value = FetchResult.LocalizedError(R.string.stripe_customer_not_found)
                    return@launch
                }

                paymentMethodSet.value = FetchResult.Success(result.value)
                AccountStore.customer = result.value

                withContext(Dispatchers.IO) {
                    fileCache.saveText(CacheFileNames.stripeCustomer, result.raw)
                }
            } catch (e: ClientError) {

                paymentMethodSet.value = if (e.statusCode == 404) {
                    FetchResult.LocalizedError(R.string.stripe_customer_not_found)
                } else {
                    FetchResult.fromServerError(e)
                }
            } catch (e: Exception) {
                info(e)
                paymentMethodSet.value = FetchResult.fromException(e)
            }
        }
    }
}
