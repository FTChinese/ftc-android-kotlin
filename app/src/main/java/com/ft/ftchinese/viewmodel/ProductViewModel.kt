package com.ft.ftchinese.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.subscription.Paywall
import com.ft.ftchinese.model.subscription.Plan
import com.ft.ftchinese.model.subscription.paywallCacheName
import com.ft.ftchinese.repository.Fetch
import com.ft.ftchinese.repository.SubscribeApi
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.util.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

/**
 * Used by ProductFragment to pass information to host
 * activity which product is selected.
 */
class ProductViewModel(
    private val cache: FileCache
) : ViewModel(), AnkoLogger {

    val selected: MutableLiveData<Plan> by lazy {
        MutableLiveData<Plan>()
    }

    val inputEnabled: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    val isNetworkAvailable = MutableLiveData<Boolean>()

    val paywallResult: MutableLiveData<Result<Paywall>> by lazy {
        MutableLiveData<Result<Paywall>>()
    }

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
                    paywallResult.value = Result.Success(paywall)
                    cachedFound = true
                }
            } catch (e: Exception) {
                info(e)
            }

            if (isNetworkAvailable.value != true) {

                if (!cachedFound) {
                    paywallResult.value = Result.LocalizedError(R.string.prompt_no_network)
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
                    paywallResult.value = Result.LocalizedError(R.string.api_server_error)
                    return@launch
                }

                paywallResult.value = Result.Success(paywall)

                cache.saveText(paywallCacheName, data!!)

            } catch (e: Exception) {
                info(e)
                paywallResult.value = parseException(e)
            }
        }
    }
}
