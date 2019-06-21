package com.ft.ftchinese.ui.channel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.model.ChannelSource
import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.util.FileCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChannelViewModel(val cache: FileCache) : ViewModel() {

    val cacheFound = MutableLiveData<Boolean>()
    val remoteResult = MutableLiveData<String>()
    val renderResult = MutableLiveData<RenderResult>()

    private var template: String? = null

    fun loadFromCache(channelSource: ChannelSource) {
        viewModelScope.launch {
            val cacheFrag = withContext(Dispatchers.IO) {
                cache.loadText(channelSource.fileName)
            }

            cacheFound.value = !cacheFrag.isNullOrBlank()

            if (cacheFrag.isNullOrBlank()) {
                return@launch
            }

            val html = render(channelSource, cacheFrag)

            renderResult.value = RenderResult(
                    success = html
            )
        }
    }

    fun loadFromServer(channelSource: ChannelSource, url: String) {
        viewModelScope.launch {
            try {
                val remoteFrag = withContext(Dispatchers.IO) {
                    Fetch().get(url).responseString()
                }

                remoteResult.value = remoteFrag

                if (remoteFrag == null) {
                    return@launch
                }


                val html = render(channelSource, remoteFrag)

                renderResult.value = RenderResult(
                        success = html
                )

            } catch (e: Exception) {
                renderResult.value = RenderResult(
                        exception = e
                )
            }
        }
    }

    private suspend fun render(channelSource: ChannelSource, htmlFragment: String) = withContext(Dispatchers.Default) {
        if (template == null) {
            template = cache.readChannelTemplate()
        }

        channelSource.render(template, htmlFragment)
    }

    fun cacheData(fileName: String, data: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                cache.saveText(fileName, data)
            }
        }
    }
}
