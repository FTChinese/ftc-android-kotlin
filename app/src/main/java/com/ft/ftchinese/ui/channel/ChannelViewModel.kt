package com.ft.ftchinese.ui.channel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.ChannelSource
import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.util.FileCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class ChannelViewModel(val cache: FileCache) :
        ViewModel(), AnkoLogger {

    val cacheFound = MutableLiveData<Boolean>()
    val renderResult = MutableLiveData<RenderResult>()

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

            renderResult.value = RenderResult(
                    success = html
            )
        }
    }

    fun loadFromServer(channelSource: ChannelSource, shouldRender: Boolean = true) {
        val url = channelSource.normalizedUrl()
        if (url == null) {
            renderResult.value = RenderResult(
                    error = R.string.api_empty_url
            )
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
                    renderResult.value = RenderResult(
                            error = R.string.api_server_error
                    )
                    return@launch
                }

                val fileName = channelSource.fileName
                if (fileName != null) {
                    launch(Dispatchers.IO) {
                        cache.saveText(fileName, remoteFrag)
                    }
                }

                if (!shouldRender) {
                    renderResult.value = RenderResult(
                            success = null
                    )
                    return@launch
                }

                val html = render(channelSource, remoteFrag)

                renderResult.value = RenderResult(
                        success = html
                )

            } catch (e: Exception) {
                info(e)
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
