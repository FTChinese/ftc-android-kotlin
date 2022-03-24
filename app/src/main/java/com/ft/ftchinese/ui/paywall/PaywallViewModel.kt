package com.ft.ftchinese.ui.paywall

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.fetch.json
import com.ft.ftchinese.model.paywall.Paywall
import com.ft.ftchinese.model.paywall.PaywallCache
import com.ft.ftchinese.model.paywall.StripePriceStore
import com.ft.ftchinese.model.paywall.defaultPaywall
import com.ft.ftchinese.model.stripesubs.StripePrice
import com.ft.ftchinese.repository.PaywallClient
import com.ft.ftchinese.repository.StripeClient
import com.ft.ftchinese.store.CacheFileNames
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.ui.base.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "PaywallViewModel"

class PaywallViewModel(
    private val cache: FileCache
) : BaseViewModel() {

    var paywallState by mutableStateOf(defaultPaywall)
        private set

    var stripeState by mutableStateOf(mapOf<String, StripePrice>())

    var msgId by mutableStateOf<Int?>(null)
        private set

    var errMsg by mutableStateOf<String?>(null)
        private set

    val paywallResult: MutableLiveData<FetchResult<Paywall>> by lazy {
        MutableLiveData<FetchResult<Paywall>>()
    }

    val stripePrices: MutableLiveData<FetchResult<List<StripePrice>>> by lazy {
        MutableLiveData<FetchResult<List<StripePrice>>>()
    }

    // The cached file are versioned therefore whenever a user
    // updates the app, the files retrieves by previous versions
    // will be ignored.
    private suspend fun loadCachedPaywall(isTest: Boolean): Paywall? {
        return withContext(Dispatchers.IO) {
            val data = cache.loadText(CacheFileNames.paywallFile(isTest))

            if (!data.isNullOrBlank()) {
                try {
                    json.parse<Paywall>(data)?.let {
                        PaywallCache.setFtc(it)
                        it
                    }
                } catch (e: Exception) {
                    e.message?.let { Log.i(TAG, it) }
                    null
                }
            } else {
                null
            }
        }
    }

    private suspend fun loadRemotePaywall(isTest: Boolean): FetchResult<Paywall> {
        if (isNetworkAvailable.value != true) {
            return FetchResult.LocalizedError(R.string.prompt_no_network)
        }

        try {
            val pwResp = withContext(Dispatchers.IO) {
                PaywallClient.retrieve(isTest)
            }

            Log.i(TAG, "Loading paywall from server finished")

            if (pwResp.body == null) {
                return FetchResult.LocalizedError(R.string.api_server_error)
            }

            Log.i(TAG, "Paywall data loaded")

            if (pwResp.raw.isNotBlank()) {
                viewModelScope.launch(Dispatchers.IO) {
                    Log.i(TAG, "Caching paywall data to file")
                    cache.saveText(CacheFileNames.paywallFile(isTest), pwResp.raw)
                }
            }

            PaywallCache.setFtc(pwResp.body)

            return FetchResult.Success(pwResp.body)
        } catch (e: Exception) {
            e.message?.let { Log.i(TAG, it) }
            return FetchResult.fromException(e)
        }
    }

    fun refreshFtcPrice(isTest: Boolean) {
        viewModelScope.launch {
            val result = loadRemotePaywall(isTest)
            paywallResult.value = result
        }
    }

    // Load paywall data from cache and then from server.
    fun loadPaywall(isTest: Boolean) {
        viewModelScope.launch {

            val pw = loadCachedPaywall(isTest)
            if (pw != null) {
                Log.i(TAG, "Paywall data loaded from local cached file")
                paywallResult.value = FetchResult.Success(pw)
                paywallState = pw
            }

            val result = loadRemotePaywall(isTest)
            paywallResult.value = result
            if (result is FetchResult.Success) {
                paywallState = result.data
            }
            when (result) {
                is FetchResult.Success -> {
                    paywallState = result.data
                }
                is FetchResult.LocalizedError -> {
                    msgId = result.msgId
                }
                is FetchResult.Error -> {
                    errMsg = result.exception.message
                }
            }
        }
    }

    private suspend fun loadCachedStripe(): List<StripePrice>? {
        Log.i(TAG, "Loading stripe prices from cache...")
        return withContext(Dispatchers.IO) {
            val data = cache.loadText(CacheFileNames.stripePrices)

            if (data == null) {
                Log.i(TAG, "Stripe prices not found in cache")
                null
            } else {
                try {
                    json.parseArray<StripePrice>(data)?.let {
                        StripePriceStore.set(it)

                        it
                    }
                } catch (e: Exception) {
                    e.message?.let { Log.i(TAG, it) }

                    null
                }
            }
        }
    }

    private suspend fun loadRemoteStripe(): FetchResult<List<StripePrice>> {
        if (isNetworkAvailable.value != true) {
            return FetchResult.LocalizedError(R.string.prompt_no_network)
        }

        try {
            Log.i(TAG, "Retrieving stripe prices from server")
            val resp = withContext(Dispatchers.IO) {
                StripeClient.listPrices()
            }

            if (resp.body == null) {
                return FetchResult.LocalizedError(R.string.api_server_error)
            }

            if (resp.raw.isNotBlank()) {
                viewModelScope.launch(Dispatchers.IO) {
                    cache.saveText(CacheFileNames.stripePrices, resp.raw)
                }
            }

            val prices = json.parseArray<StripePrice>(resp.body)
                ?: return FetchResult.Error(Exception("JSON parsing failed"))
            StripePriceStore.set(prices)

            return FetchResult.Success(prices)
        } catch (e: Exception) {
            e.message?.let { Log.i(TAG, "Error retrieving stripe prices: $it") }
            return FetchResult.fromException(e)
        }
    }

    fun refreshStripePrices() {
        viewModelScope.launch {
            val result = loadRemoteStripe()
            stripePrices.value = result
            progressLiveData.value = false
        }
    }

    // Load stripe prices from cache, or from server if cached data not found,
    // before show stripe payment page.
    // This works as a backup in case stripe prices is not yet
    // loaded into memory.
    fun loadStripePrices() {
        Log.i(TAG, "Loading stripe prices...")
        progressLiveData.value = true

        viewModelScope.launch {
            val prices = loadCachedStripe()
            if (prices != null) {
                stripePrices.value = FetchResult.Success(prices)
                progressLiveData.value = false
            }

            val result = loadRemoteStripe()
            stripePrices.value = result
            progressLiveData.value = false

            when (result) {
                is FetchResult.Success -> {
                    stripeState = result.data.associateBy {
                        it.id
                    }
                }
                is FetchResult.LocalizedError -> {
                    msgId = result.msgId
                }
                is FetchResult.Error -> {
                    errMsg = result.exception.message
                }
            }
        }
    }

    fun ensureStripePrices() {
        if (!StripePriceStore.isEmpty) {
            return
        }

        loadStripePrices()
    }
}
