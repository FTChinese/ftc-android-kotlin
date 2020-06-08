package com.ft.ftchinese.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.content.ChannelSource
import com.ft.ftchinese.repository.Fetch
import com.ft.ftchinese.store.FileCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class ChannelViewModel(val cache: FileCache) :
        ViewModel(), AnkoLogger {

    val isNetworkAvailable = MutableLiveData<Boolean>()
    val contentResult: MutableLiveData<Result<String>> by lazy {
        MutableLiveData<Result<String>>()
    }

    fun load(channelSource: ChannelSource, bustCache: Boolean) {
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
                        contentResult.value = Result.Success(data)

                        if (isNetworkAvailable.value != true) {
                            return@launch
                        }

                        // Background update cache.
                        val url = channelSource.normalizedUrl() ?: return@launch

                        info("Start background update from $url for $cacheName")
                       try {
                            withContext(Dispatchers.IO) {
                                val remoteFrag = Fetch().get(url).responseString() ?: return@withContext
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

            val url = channelSource.normalizedUrl()
            info("Channel cache not found. Loading from $url")

            if (url.isNullOrBlank()) {
                contentResult.value = Result.LocalizedError(R.string.api_empty_url)
                return@launch
            }

            if (isNetworkAvailable.value != true) {
                contentResult.value = Result.LocalizedError(R.string.prompt_no_network)
                return@launch
            }

            try {
                val remoteFrag = withContext(Dispatchers.IO) {
                    Fetch().get(url).responseString()
                }

                if (remoteFrag.isNullOrBlank()) {
                    info("Channel fragment is empty")
                    contentResult.value = Result.LocalizedError(R.string.api_server_error)
                    return@launch
                }

                contentResult.value = Result.Success(remoteFrag)

                if (!cacheName.isNullOrBlank()) {
                    launch(Dispatchers.IO) {
                        cache.saveText(cacheName, remoteFrag)
                    }
                }

            } catch (e: Exception) {
                contentResult.value = parseException(e)
            }
        }
    }
}
