package com.ft.ftchinese.ui.checkout

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.content.StoryBuilder
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Address
import com.ft.ftchinese.repository.AccountRepo
import com.ft.ftchinese.repository.Config
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.ui.base.BaseViewModel
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class BuyerInfoViewModel : BaseViewModel(), AnkoLogger {

    val htmlRendered = MutableLiveData<FetchResult<String>>()

    /**
     * @param action - `buy` or `renew`.
     */
    fun loadPage(account: Account, cache: FileCache, action: String) {
        if (isNetworkAvailable.value == false) {
            return
        }

        val uri = Config.buildSubsConfirmUrl(account, action)
        if (uri == null) {
            info("Address url is empty")
            htmlRendered.value = FetchResult.LocalizedError(R.string.loading_failed)
            return
        }

        info("Fetching address page from ${uri.toString()}")

        progressLiveData.value = true
        viewModelScope.launch {

            try {
                val webContentAsync = async(Dispatchers.IO) {
                    Fetch()
                        .get(uri.toString())
                        .endPlainText()
                }
                val addressAsync = async(Dispatchers.IO) {
                    info("Fetching address...")
                    AccountRepo.loadAddress(account.id)
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
                info(e)
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
            StoryBuilder(template)
                .withUserInfo(account)
                .withAddress(address)
                .withChannel(content)
                .render()
        }
    }
}
