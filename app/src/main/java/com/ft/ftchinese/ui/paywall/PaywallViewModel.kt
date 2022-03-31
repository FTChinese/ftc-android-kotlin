package com.ft.ftchinese.ui.paywall

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.fetch.json
import com.ft.ftchinese.model.paywall.*
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.stripesubs.StripePrice
import com.ft.ftchinese.repository.PaywallClient
import com.ft.ftchinese.repository.StripeClient
import com.ft.ftchinese.store.CacheFileNames
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.store.PaywallCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "PaywallViewModel"

class PaywallViewModel(application: Application) : AndroidViewModel(application) {

    private val cache = FileCache(application)
    private var premiumOnTop = false

    val isNetworkAvailable = MutableLiveData<Boolean>()
    val progressLiveData = MutableLiveData(false)
    val refreshingLiveData = MutableLiveData(false)

    val ftcPriceLiveData: MutableLiveData<Paywall> by lazy {
        MutableLiveData<Paywall>(defaultPaywall)
    }

    val stripePriceLiveData: MutableLiveData<Map<String, StripePrice>> by lazy {
        MutableLiveData<Map<String, StripePrice>>()
    }

    var msgId by mutableStateOf<Int?>(null)
        private set

    var errMsg by mutableStateOf<String?>(null)
        private set

    @Deprecated("")
    val stripePrices: MutableLiveData<FetchResult<List<StripePrice>>> by lazy {
        MutableLiveData<FetchResult<List<StripePrice>>>()
    }

    fun putPremiumOnTop(onTop: Boolean) {
        if (onTop == premiumOnTop) {
            return
        }

        premiumOnTop = onTop
        ftcPriceLiveData.value?.let {
            ftcPriceLiveData.value = it.reOrderProducts(onTop)
        }
    }

    fun ftcCheckoutItem(priceId: String, m: Membership): CartItemFtcV2? {
        val price = ftcPriceLiveData.value?.products
            ?.flatMap { it.prices }
            ?.findLast { it.id == priceId }
            ?: return null

        return price.buildCartItem(m)
    }

    fun stripeCheckoutItem(priceId:  String, trialId: String?, m: Membership): CartItemStripeV2? {
        val price = stripePriceLiveData.value?.get(priceId) ?: return null

        return CartItemStripeV2(
            intent = price.checkoutIntent(m),
            recurring = price,
            trial = trialId?.let { stripePriceLiveData.value?.get(it) }
        )
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

            return FetchResult.Success(pwResp.body)
        } catch (e: Exception) {
            e.message?.let { Log.i(TAG, it) }
            return FetchResult.fromException(e)
        }
    }

    private fun setFtcPrice(result: FetchResult<Paywall>) {
        when (result) {
            is FetchResult.Success -> {
                ftcPriceLiveData.value = result.data.reOrderProducts(premiumOnTop)
                PaywallCache.setFtc(result.data)
            }
            is FetchResult.LocalizedError -> {
                msgId = result.msgId
            }
            is FetchResult.Error -> {
                errMsg = result.exception.message
            }
        }
    }

    // Load paywall data from cache and then from server.
    fun loadPaywall(isTest: Boolean) {
        viewModelScope.launch {

            val pw = loadCachedPaywall(isTest)
            if (pw != null) {
                Log.i(TAG, "Paywall data loaded from local cached file")
                setFtcPrice(FetchResult.Success(pw))
            }

            val result = loadRemotePaywall(isTest)

            setFtcPrice(result)
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

    private fun setStripePrice(result: FetchResult<List<StripePrice>>) {
        when (result) {
            is FetchResult.Success -> {
                result.data.associateBy {
                    it.id
                }.let {
                    stripePriceLiveData.value = it
                    PaywallCache.setStripe(it)
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

    // Load stripe prices from cache, or from server if cached data not found,
    // before show stripe payment page.
    // This works as a backup in case stripe prices is not yet
    // loaded into memory.
    fun loadStripePrices() {
        progressLiveData.value = true

        viewModelScope.launch {
            val prices = loadCachedStripe()
            if (prices != null) {
                progressLiveData.value = false
                setStripePrice(FetchResult.Success(prices))
            }

            val result = loadRemoteStripe()
            progressLiveData.value = false
            setStripePrice(result)
        }
    }

    fun refresh() {
        if (isNetworkAvailable.value != true) {
            msgId = R.string.prompt_no_network
            return
        }

        refreshingLiveData.value = true

        viewModelScope.launch {
            val ftcDeferred = async { loadRemotePaywall(false) }
            val stripeDeferred = async { loadRemoteStripe() }

            val ftcRes = ftcDeferred.await()
            val stripeRes = stripeDeferred.await()

            setFtcPrice(ftcRes)
            setStripePrice(stripeRes)

            refreshingLiveData.value = false
            msgId = R.string.refresh_success
        }
    }

    @Deprecated("")
    fun ensureStripePrices() {
        if (!StripePriceStore.isEmpty) {
            return
        }

        loadStripePrices()
    }
}
