package com.ft.ftchinese.ui.paywall

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.fetch.marshaller
import com.ft.ftchinese.model.paywall.CartItemFtc
import com.ft.ftchinese.model.paywall.CartItemStripe
import com.ft.ftchinese.model.paywall.Paywall
import com.ft.ftchinese.model.paywall.defaultPaywall
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.stripesubs.StripePrice
import com.ft.ftchinese.repository.PaywallClient
import com.ft.ftchinese.repository.StripeClient
import com.ft.ftchinese.store.CacheFileNames
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.tracking.AddCartParams
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.ui.components.ToastMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString

private const val TAG = "PaywallViewModel"

class PaywallViewModel(application: Application) : AndroidViewModel(application) {

    private val tracker = StatsTracker.getInstance(application)
    private val cache = FileCache(application)
    private var premiumOnTop = false

    val isNetworkAvailable = MutableLiveData(application.isConnected)
    val progressLiveData = MutableLiveData(false)
    val refreshingLiveData = MutableLiveData(false)

    val ftcPriceLiveData: MutableLiveData<Paywall> by lazy {
        MutableLiveData<Paywall>(defaultPaywall)
    }

    val stripePriceLiveData: MutableLiveData<Map<String, StripePrice>> by lazy {
        MutableLiveData<Map<String, StripePrice>>()
    }

    val toastLiveData: MutableLiveData<ToastMessage> by lazy {
        MutableLiveData<ToastMessage>()
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

    fun ftcCheckoutItem(priceId: String, m: Membership): CartItemFtc? {
        val price = ftcPriceLiveData.value?.products
            ?.flatMap { it.prices }
            ?.findLast { it.id == priceId }
            ?: return null

        return price.buildCartItem(m)
    }

    fun stripeCheckoutItem(priceId:  String, trialId: String?, m: Membership): CartItemStripe? {
        val price = stripePriceLiveData.value?.get(priceId) ?: return null

        return CartItemStripe(
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
                    marshaller.decodeFromString<Paywall>(data)
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
                    cache.saveFtcPrice(isTest, pwResp.raw)
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
                stripePriceLiveData.value = result.data.stripe.associateBy({
                    it.price.id
                }, {
                    it.price
                })
            }
            is FetchResult.LocalizedError -> {
                toastLiveData.value = ToastMessage.Resource(result.msgId)
            }
            is FetchResult.TextError -> {
                toastLiveData.value = ToastMessage.Text(result.text)
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

            if (isNetworkAvailable.value != true) {
                return@launch
            }

            val result = loadRemotePaywall(isTest)

            setFtcPrice(result)
        }
    }

    fun refresh(isTest: Boolean) {
        if (isNetworkAvailable.value != true) {
            toastLiveData.value = ToastMessage.Resource(R.string.prompt_no_network)
            return
        }

        refreshingLiveData.value = true

        viewModelScope.launch {
//            val ftcDeferred = async { loadRemotePaywall(false) }
//            val stripeDeferred = async { loadRemoteStripe() }
//
//            val ftcRes = ftcDeferred.await()
//            val stripeRes = stripeDeferred.await()
//
            val ftcRes = loadRemotePaywall(isTest)
            setFtcPrice(ftcRes)
//            setStripePrice(stripeRes)

            refreshingLiveData.value = false
            toastLiveData.value = ToastMessage.Resource(R.string.refresh_success)
        }
    }

    @Deprecated("")
    private suspend fun loadCachedStripe(): List<StripePrice>? {
        Log.i(TAG, "Loading stripe prices from cache...")
        return withContext(Dispatchers.IO) {
            val data = cache.loadText(CacheFileNames.stripePrices)

            if (data == null) {
                Log.i(TAG, "Stripe prices not found in cache")
                null
            } else {
                try {
                    marshaller.decodeFromString(data)
                } catch (e: Exception) {
                    e.message?.let { Log.i(TAG, it) }

                    null
                }
            }
        }
    }

    @Deprecated("")
    private suspend fun loadRemoteStripe(): FetchResult<List<StripePrice>> {

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
                    cache.saveStripePrice(resp.raw)
                }
            }

            return FetchResult.Success(resp.body)
        } catch (e: Exception) {
            e.message?.let { Log.i(TAG, "Error retrieving stripe prices: $it") }
            return FetchResult.fromException(e)
        }
    }

    @Deprecated("")
    private fun setStripePrice(result: FetchResult<List<StripePrice>>) {
        when (result) {
            is FetchResult.Success -> {
                result.data.associateBy {
                    it.id
                }.let {
                    stripePriceLiveData.value = it
                }
            }
            is FetchResult.LocalizedError -> {
                toastLiveData.value = ToastMessage.Resource(result.msgId)
            }
            is FetchResult.TextError -> {
                toastLiveData.value = ToastMessage.Text(result.text)
            }
        }
    }

    // Load stripe prices from cache, or from server if cached data not found,
    // before show stripe payment page.
    // This works as a backup in case stripe prices is not yet
    // loaded into memory.
    @Deprecated("")
    fun loadStripePrices() {

        viewModelScope.launch {
            val prices = loadCachedStripe()
            if (prices != null) {
                progressLiveData.value = false
                setStripePrice(FetchResult.Success(prices))
            }

            if (isNetworkAvailable.value != true) {
                return@launch
            }

            val result = loadRemoteStripe()

            setStripePrice(result)
        }
    }

    fun trackDisplayPaywall() {
        tracker.displayPaywall()
    }

    fun trackAddCart(params: AddCartParams) {
        tracker.addCart(params)
    }
}
