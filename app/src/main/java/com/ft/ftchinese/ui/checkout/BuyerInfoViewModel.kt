package com.ft.ftchinese.ui.checkout

import android.app.Application
import android.util.Log
import android.webkit.JavascriptInterface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.content.TemplateBuilder
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Address
import com.ft.ftchinese.repository.AccountRepo
import com.ft.ftchinese.repository.Config
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.store.InvoiceStore
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.isConnected
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "BuyerInfoViewModel"

class BuyerInfoViewModel(application: Application) : AndroidViewModel(application) {

    private val session = SessionManager.getInstance(application)
    private val fileCache = FileCache(application)
    private val invoiceStore = InvoiceStore.getInstance(application)

    val progressLiveData = MutableLiveData<Boolean>()
    val isNetworkAvailable = MutableLiveData(application.isConnected)
    val htmlRendered = MutableLiveData<FetchResult<String>>()
    val exitLiveData = MutableLiveData(false)
    val alertLiveData = MutableLiveData("")

    /**
     * @param action - `buy` or `renew`.
     */
    fun loadPage() {
//        htmlRendered.value = FetchResult.Success(fileCache.readTestHtml())
//        progressLiveData.value = false
//        return

        if (isNetworkAvailable.value == false) {
            return
        }

        val account = session.loadAccount()
        val action = invoiceStore.loadPurchaseAction()
        if (account == null || action == null) {
            exitLiveData.value = true
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

    private suspend fun render(account: Account, content: String, address: Address): String {
        val template = withContext(Dispatchers.IO) {
            fileCache.readChannelTemplate()
        }

        return withContext(Dispatchers.Default) {
            TemplateBuilder(template)
                .withUserInfo(account)
                .withAddress(address)
                .withChannel(content)
                .render()
        }
    }

    fun clearAlert() {
        alertLiveData.value = ""
    }

    @JavascriptInterface
    fun wvClosePage() {
        exitLiveData.postValue(true)
    }

    @JavascriptInterface
    fun wvProgress(loading: Boolean = false) {
        progressLiveData.postValue(loading)
    }

    @JavascriptInterface
    fun wvAlert(msg: String) {
        alertLiveData.postValue(msg)
    }
}
