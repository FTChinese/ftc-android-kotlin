package com.ft.ftchinese.ui.paywall

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.fetch.json
import com.ft.ftchinese.model.ftcsubs.*
import com.ft.ftchinese.model.paywall.Paywall
import com.ft.ftchinese.model.paywall.PaywallCache
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.stripesubs.StripePrice
import com.ft.ftchinese.model.stripesubs.StripePriceStore
import com.ft.ftchinese.repository.PaywallClient
import com.ft.ftchinese.repository.StripeClient
import com.ft.ftchinese.store.CacheFileNames
import com.ft.ftchinese.store.FileCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class PaywallViewModel(
    private val cache: FileCache
) : ViewModel(), AnkoLogger {
    val isNetworkAvailable = MutableLiveData<Boolean>()

    val paywallResult: MutableLiveData<FetchResult<Paywall>> by lazy {
        MutableLiveData<FetchResult<Paywall>>()
    }

    val stripePrices: MutableLiveData<FetchResult<List<StripePrice>>> by lazy {
        MutableLiveData<FetchResult<List<StripePrice>>>()
    }

    // The cached file are versioned therefore whenever a user
    // updates the app, the files retrieves by previous versions
    // will be ignored.
    private suspend fun getCachedPaywall(isTest: Boolean): Paywall? {
        return withContext(Dispatchers.IO) {
            val data = cache.loadText(CacheFileNames.paywallFile(isTest))

            if (!data.isNullOrBlank()) {
                try {
                    json.parse<Paywall>(data)
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        }
    }

    // Load paywall data from cache and then from server.
    fun loadPaywall(isRefreshing: Boolean, account: Account?) {
        val isTest = account?.isTest ?: false

        viewModelScope.launch {

            // If not manually refreshing
            if (!isRefreshing) {
                val pw = getCachedPaywall(isTest)
                if (pw != null) {
                    paywallResult.value = FetchResult.Success(pw)
                    // Update the in-memory cache.
                    PaywallCache.update(pw)
                }
            }

            // Always retrieve from api.
            if (isNetworkAvailable.value != true) {
                paywallResult.value = FetchResult.LocalizedError(R.string.prompt_no_network)
                return@launch
            }

            try {
                val paywall = withContext(Dispatchers.IO) {
                    PaywallClient.retrieve(account?.isTest ?: false)
                }

                if (paywall == null) {
                    paywallResult.value = FetchResult.LocalizedError(R.string.api_server_error)
                    return@launch
                }
                
                paywallResult.value = FetchResult.Success(paywall.value)
                PaywallCache.update(paywall.value)

                withContext(Dispatchers.IO) {
                    cache.saveText(CacheFileNames.paywallFile(isTest), paywall.raw)
                }

            } catch (e: Exception) {
                info(e)
                paywallResult.value = FetchResult.fromException(e)
            }
        }
    }

    // Retrieve stripe prices in background and refresh cache.
    // It will be executed whenever user opened MemberActivity or PaywallActivity.
    fun refreshStripePrices() {
        info("Retrieving stripe prices in background...")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = StripeClient.listPrices() ?: return@launch
                StripePriceStore.add(result.value)
                cache.saveText(CacheFileNames.stripePrices, result.raw)
                info("Stripe prices cached...")
            } catch (e: Exception) {
                info(e)
            }
        }
    }

    private suspend fun stripeCachedPrices(): List<StripePrice>? {
        info("Loading stripe prices from cache...")
        return withContext(Dispatchers.IO) {
            val data = cache.loadText(CacheFileNames.stripePrices)

            if (data == null) {
                info("Stripe prices not found in cache")
                null
            } else {
                try {
                    json.parseArray(data)
                } catch (e: Exception) {
                    info(e)
                    null
                }
            }
        }
    }

    // Load stripe prices from cache, or from server is cached data not found,
    // before show stripe payment page.
    // This works as a backup in case stripe prices is not yet
    // loaded into memory.
    fun loadStripePrices() {
        info("Loading stripe prices...")
        viewModelScope.launch {
            val prices = stripeCachedPrices()
            if (prices != null) {
                stripePrices.value = FetchResult.Success(prices)
                return@launch
            }

            // Retrieve server data
            try {
                info("Retrieving stripe prices from server")
                val result = withContext(Dispatchers.IO) {
                    StripeClient.listPrices()
                }

                info("Stripe prices retrieval failed")
                if (result == null) {
                    stripePrices.value = FetchResult.LocalizedError(R.string.api_server_error)
                    return@launch
                }

                stripePrices.value = FetchResult.Success(result.value)

                withContext(Dispatchers.IO) {
                    cache.saveText(CacheFileNames.stripePrices, result.raw)
                    info("Cached stripe prices")
                }
            }
            catch (e: Exception) {
                info(e)
                stripePrices.value = FetchResult.fromException(e)
            }
        }
    }
}
