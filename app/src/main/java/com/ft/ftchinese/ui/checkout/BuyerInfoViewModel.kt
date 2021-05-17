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
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.ui.base.BaseViewModel
import kotlinx.coroutines.*

class BuyerInfoViewModel : BaseViewModel() {

    val htmlRendered = MutableLiveData<String>()

    suspend private fun asyncLoadAddress(account: Account): Address? {
        return try {
            withContext(Dispatchers.IO) {
                AccountRepo.loadAddress(account.id)
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend private fun asyncCrawlWeb(url: String): String? {
        return try {
            Fetch()
                .get(url)
                .endPlainText()
        } catch (e: Exception) {
            null
        }
    }

    fun loadPage(account: Account, url: String) {
        if (isNetworkAvailable.value == false) {
            return
        }

        viewModelScope.launch {

            val deferreds = listOf(
                async {
                    asyncCrawlWeb(url)
                },
                async {
                    asyncLoadAddress(account)
                }
            )

            val (webContent, addr) = deferreds.awaitAll()
        }
    }

    private suspend fun render(content: String, addr: Address, cache: FileCache): String {
        val template = withContext(Dispatchers.IO) {
            cache.readChannelTemplate()
        }

        val html = withContext(Dispatchers.Default) {
            StoryBuilder(template)
                .render()
        }

        return html
    }
}
