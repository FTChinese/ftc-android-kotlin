package com.ft.ftchinese.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.subscription.StripeCustomer
import com.ft.ftchinese.model.fetch.ClientError
import com.ft.ftchinese.repository.StripeClient
import com.ft.ftchinese.store.AccountStore
import com.ft.ftchinese.store.CacheFileNames
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.model.fetch.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class CustomerViewModel(
    private val fileCache: FileCache
) : ViewModel(), AnkoLogger {

    val isNetworkAvailable = MutableLiveData<Boolean>()

    val customerCreated: MutableLiveData<Result<StripeCustomer>> by lazy {
        MutableLiveData<Result<StripeCustomer>>()
    }

    val customerRetrieved: MutableLiveData<Result<StripeCustomer>> by lazy {
        MutableLiveData<Result<StripeCustomer>>()
    }

    val paymentMethodSet: MutableLiveData<Result<StripeCustomer>> by lazy {
        MutableLiveData<Result<StripeCustomer>>()
    }

    // Create stripe customer.
    fun create(account: Account) {
        if (isNetworkAvailable.value == false) {
            customerCreated.value = Result.LocalizedError(R.string.prompt_no_network)
            return
        }

        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    StripeClient.createCustomer(account)
                }

                if (result == null) {

                    customerCreated.value = Result.LocalizedError(R.string.stripe_customer_not_created)
                    return@launch
                }

                result.value.let {
                    customerCreated.value = Result.Success(it)
                    AccountStore.customer = it
                }

                withContext(Dispatchers.IO) {
                    fileCache.saveText(CacheFileNames.stripeCustomer, result.raw)
                }

            } catch (e: ClientError) {

                customerCreated.value = if (e.statusCode == 404) {
                    Result.LocalizedError(R.string.stripe_customer_not_found)
                } else {
                    parseApiError(e)
                }
            } catch (e: Exception) {

                customerCreated.value = parseException(e)
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
                customerRetrieved.value = Result.Success(customer)
                AccountStore.customer = customer
                return@launch
            }

            if (isNetworkAvailable.value != true) {
                customerRetrieved.value = Result.LocalizedError(R.string.api_network_failure)
                return@launch
            }

            try {
                val result = withContext(Dispatchers.IO) {
                    StripeClient.retrieveCustomer(account)
                }

                if (result == null) {
                    customerRetrieved.value = Result.LocalizedError(R.string.stripe_customer_not_found)
                    return@launch
                }

                customerRetrieved.value = Result.Success(result.value)
                AccountStore.customer = result.value

                withContext(Dispatchers.IO) {
                    fileCache.saveText(CacheFileNames.stripeCustomer, result.raw)
                }
            } catch (e: Exception) {
                customerRetrieved.value = parseException(e)
            }
        }
    }


    // Set default payment method.
    fun setDefaultPaymentMethod(account: Account, pmId: String) {
        if (isNetworkAvailable.value == false) {
            paymentMethodSet.value = Result.LocalizedError(R.string.prompt_no_network)
            return
        }

        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    StripeClient.setDefaultPaymentMethod(account, pmId)
                }

                if (result == null) {
                    paymentMethodSet.value = Result.LocalizedError(R.string.stripe_customer_not_found)
                    return@launch
                }

                paymentMethodSet.value = Result.Success(result.value)
                AccountStore.customer = result.value

                withContext(Dispatchers.IO) {
                    fileCache.saveText(CacheFileNames.stripeCustomer, result.raw)
                }
            } catch (e: ClientError) {

                paymentMethodSet.value = if (e.statusCode == 404) {
                    Result.LocalizedError(R.string.stripe_customer_not_found)
                } else {
                    parseApiError(e)
                }
            } catch (e: Exception) {
                info(e)
                paymentMethodSet.value = parseException(e)
            }
        }
    }
}
