package com.ft.ftchinese.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.ChannelSource
import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.store.FileCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class ChannelViewModel(val cache: FileCache) :
        ViewModel(), AnkoLogger {

    val cacheFound = MutableLiveData<Boolean>()
    val renderResult: MutableLiveData<Result<String>> by lazy {
        MutableLiveData<Result<String>>()
    }

    private var template: String? = null

    fun loadFromCache(channelSource: ChannelSource) {
        viewModelScope.launch {
            val cacheFrag = withContext(Dispatchers.IO) {
                cache.loadText(channelSource.fileName)
            }

            if (cacheFrag.isNullOrBlank()) {
                cacheFound.value = false
                return@launch
            }

            cacheFound.value = true

            val html = render(channelSource, cacheFrag)

            if (html == null) {
                renderResult.value = Result.LocalizedError(R.string.loading_failed)
                return@launch
            }

            renderResult.value = Result.Success(html)
        }
    }

    fun loadFromServer(channelSource: ChannelSource, shouldRender: Boolean = true) {
        val url = channelSource.normalizedUrl()
        if (url == null) {
            renderResult.value = Result.LocalizedError(R.string.api_empty_url)
            return
        }

        info("Load channel from $url")

        viewModelScope.launch {
            try {
                val remoteFrag = withContext(Dispatchers.IO) {
                    Fetch().get(url).responseString()
                }

                info("Channel fragment loaded")

                if (remoteFrag.isNullOrBlank()) {
                    info("Channel fragment is empty")
                    renderResult.value = Result.LocalizedError(R.string.api_server_error)
                    return@launch
                }

                val fileName = channelSource.fileName
                if (fileName != null) {
                    launch(Dispatchers.IO) {
                        cache.saveText(fileName, remoteFrag)
                    }
                }

                if (!shouldRender) {

                    return@launch
                }

                val html = render(channelSource, remoteFrag)

                if (html == null) {
                    renderResult.value = Result.LocalizedError(R.string.loading_failed)
                    return@launch
                }
                renderResult.value = Result.Success(html)

            } catch (e: Exception) {
                info(e)
                renderResult.value = parseException(e)
            }
        }
    }

    private suspend fun render(channelSource: ChannelSource, htmlFragment: String) = withContext(Dispatchers.Default) {
        if (template == null) {
            template = cache.readChannelTemplate()
        }

        channelSource.render(template, htmlFragment)
    }
}
