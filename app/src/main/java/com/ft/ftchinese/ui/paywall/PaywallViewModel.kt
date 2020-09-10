package com.ft.ftchinese.ui.paywall

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.subscription.*
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

    val paywallResult: MutableLiveData<Result<Paywall>> by lazy {
        MutableLiveData<Result<Paywall>>()
    }

    fun loadPaywall(isRefreshing: Boolean) {
        viewModelScope.launch {

            // If not manually refreshing
            if (!isRefreshing) {
                try {
                    val pw = withContext(Dispatchers.IO) {
                        val data = cache.loadText(paywallFileName)

                        if (!data.isNullOrBlank()) {
                            json.parse<Paywall>(data)
                        } else {
                            null
                        }
                    }

                    if (pw != null) {

                        paywallResult.value = Result.Success(pw)
                        // Update the in-memory cache.
                        PlanStore.plans = pw.products.flatMap {
                            it.plans
                        }
                    }
                } catch (e: Exception) {
                    info(e)
                }
            }

            if (isNetworkAvailable.value != true) {

                paywallResult.value = Result.LocalizedError(R.string.prompt_no_network)

                return@launch
            }

            try {
                val (pw, data) = withContext(Dispatchers.IO) {

                    val (_, data) = Fetch().get(SubscribeApi.PAYWALL).endJsonText()

                    info("Fetch paywall data $data")

                    if(!data.isNullOrBlank()) {
                        Pair(json.parse<Paywall>(data), data)
                    } else {
                        null
                    }
                }
                        ?: return@launch

                if (pw == null) {
                    paywallResult.value = Result.LocalizedError(R.string.api_server_error)
                    return@launch
                }

                paywallResult.value = Result.Success(pw)

                withContext(Dispatchers.IO) {
                    cache.saveText(paywallFileName, data)
                }

            } catch (e: Exception) {
                info(e)
                paywallResult.value = parseException(e)
            }
        }
    }
}
