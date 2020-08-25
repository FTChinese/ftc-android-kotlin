package com.ft.ftchinese.ui.paywall

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.subscription.Paywall
import com.ft.ftchinese.model.subscription.paywallCacheName
import com.ft.ftchinese.repository.Fetch
import com.ft.ftchinese.repository.SubscribeApi
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.util.json
import com.ft.ftchinese.viewmodel.Result
import com.ft.ftchinese.viewmodel.parseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class PaywallViewModel(
    private val cache: FileCache
) : ViewModel(), AnkoLogger {
    val isNetworkAvailable = MutableLiveData<Boolean>()

    val result: MutableLiveData<Result<Paywall>> by lazy {
        MutableLiveData<Result<Paywall>>()
    }


    // The initial paywall data is embedded in the app.
    // Upon UI show up, it will first try to see if cache has
    // a copy of paywall data fetch from API.
    // If found, use it; otherwise use the embedded data.
    // In both case we will fetch latest data from API silently.
    // The only exception is when user manually swipe, in which
    // case we will fetch data directly from server, and UI
    // should show progress.
    fun loadPaywall() {

        var cachedFound = false
        viewModelScope.launch {
            try {
                val data = withContext(Dispatchers.IO) {
                    cache.loadText(paywallCacheName)
                }

                val paywall = if (!data.isNullOrBlank()) {
                    json.parse<Paywall>(data)
                } else {
                    null
                }

                if (paywall != null) {
                    result.value = Result.Success(paywall)
                    cachedFound = true
                }
            } catch (e: Exception) {
                info(e)
            }

            if (isNetworkAvailable.value != true) {

                if (!cachedFound) {
                    result.value = Result.LocalizedError(R.string.prompt_no_network)
                }

                return@launch
            }

            try {
                val data = withContext(Dispatchers.IO) {
                    Fetch().get(SubscribeApi.PAYWALL).responseString()
                }

                val paywall = if(data.isNullOrBlank()) {
                    null
                } else {
                    json.parse<Paywall>(data)
                }

                if (paywall == null) {
                    result.value = Result.LocalizedError(R.string.api_server_error)
                    return@launch
                }

                result.value = Result.Success(paywall)

                cache.saveText(paywallCacheName, data!!)

            } catch (e: Exception) {
                info(e)
                result.value = parseException(e)
            }
        }
    }
}
