package com.ft.ftchinese.ui.checkout

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.content.TemplateBuilder
import com.ft.ftchinese.model.enums.PurchaseAction
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Address
import com.ft.ftchinese.repository.AccountRepo
import com.ft.ftchinese.repository.Config
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.ui.base.BaseViewModel
import kotlinx.coroutines.*

class BuyerInfoViewModel : BaseViewModel() {

    val htmlRendered = MutableLiveData<FetchResult<String>>()

    /**
     * @param action - `buy` or `renew`.
     */
    fun loadPage(account: Account, cache: FileCache, action: PurchaseAction) {
        if (isNetworkAvailable.value == false) {
            return
        }

        val uri = Config.buildSubsConfirmUrl(account, action)
        if (uri == null) {
            Log.i(TAG, "Address url is empty")
            htmlRendered.value = FetchResult.LocalizedError(R.string.loading_failed)
            return
        }

        Log.i(TAG, "Fetching address page from ${uri.toString()}")

        progressLiveData.value = true
        viewModelScope.launch {

            try {
                val webContentAsync = async(Dispatchers.IO) {
                    Fetch()
                        .get(uri.toString())
                        .endText()
                        .body
                }
                val addressAsync = async(Dispatchers.IO) {
                    Log.i(TAG, "Fetching address...")
                    if (account.isFtcOnly) {
                        AccountRepo.loadAddress(account.id)
                    } else {
                        Address()
                    }
                }

                val webContent = webContentAsync.await()
                val address = addressAsync.await()

                progressLiveData.value = false

                if (webContent.isNullOrBlank() || address == null) {
                    htmlRendered.value = FetchResult.LocalizedError(R.string.loading_failed)
                    return@launch
                }
                val html = render(
                    account = account,
                    cache = cache,
                    content = webContent,
                    address = address,
                )

                htmlRendered.value = FetchResult.Success(html)
            } catch (e: Exception) {
                e.message?.let { Log.i(TAG, it) }
                progressLiveData.value = false
                htmlRendered.value = FetchResult.fromException(e)
            }
        }
    }

    private suspend fun render(account: Account, cache: FileCache, content: String, address: Address): String {
        val template = withContext(Dispatchers.IO) {
            cache.readChannelTemplate()
        }

        return withContext(Dispatchers.Default) {
            TemplateBuilder(template)
                .withUserInfo(account)
                .withAddress(address)
                .withChannel(content)
                .render()
        }
    }

    companion object {
        private const val TAG = "BuyerInfoViewModel"
    }
}
