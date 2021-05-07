package com.ft.ftchinese.ui.channel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.content.ChannelSource
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.repository.Config
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.ui.data.FetchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class ChannelViewModel(val cache: FileCache, val account: Account?) :
        ViewModel(), AnkoLogger {

    val isNetworkAvailable = MutableLiveData<Boolean>()
    val contentResult: MutableLiveData<FetchResult<String>> by lazy {
        MutableLiveData<FetchResult<String>>()
    }

    fun load(channelSource: ChannelSource, bustCache: Boolean) {
        info("Loading channel content from $channelSource")

       val cacheName = channelSource.fileName

        info("Channel page cache file: $cacheName")

        viewModelScope.launch {
            if (!cacheName.isNullOrBlank() && !bustCache) {
                try {
                    info("Loading channel cache file $cacheName")
                    val data = withContext(Dispatchers.IO) {
                        cache.loadText(cacheName)
                    }

                    if (!data.isNullOrBlank()) {
                        info("Using cached channel file $cacheName")
                        contentResult.value = FetchResult.Success(data)

                        if (isNetworkAvailable.value != true) {
                            return@launch
                        }

                        // Background update cache.
                        val url = Config.buildChannelSourceUrl(account, channelSource) ?: return@launch

                        info("Start background update from $url for $cacheName")
                       try {
                            withContext(Dispatchers.IO) {
                                val remoteFrag = Fetch().get(url.toString()).endPlainText() ?: return@withContext
                                cache.saveText(cacheName, remoteFrag)
                            }
                        } catch (e: Exception) {
                            return@launch
                        }
                        return@launch
                    }
                } catch (e: Exception) {
                    info(e)
                }
            }

            val url = Config.buildChannelSourceUrl(account, channelSource)
            info("Channel cache not found. Loading from $url")

            if (url == null) {
                contentResult.value = FetchResult.LocalizedError(R.string.api_empty_url)
                return@launch
            }

            if (isNetworkAvailable.value != true) {
                contentResult.value = FetchResult.LocalizedError(R.string.prompt_no_network)
                return@launch
            }

            try {
                val remoteFrag = withContext(Dispatchers.IO) {
                    Fetch().get(url.toString()).endPlainText()
                }

                if (remoteFrag.isNullOrBlank()) {
                    info("Channel fragment is empty")
                    contentResult.value = FetchResult.LocalizedError(R.string.api_server_error)
                    return@launch
                }

                contentResult.value = FetchResult.Success(remoteFrag)

                if (!cacheName.isNullOrBlank()) {
                    launch(Dispatchers.IO) {
                        cache.saveText(cacheName, remoteFrag)
                    }
                }

            } catch (e: Exception) {
                contentResult.value = FetchResult.fromException(e)
            }
        }
    }
}
