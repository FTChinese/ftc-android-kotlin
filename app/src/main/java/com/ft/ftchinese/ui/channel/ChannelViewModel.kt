package com.ft.ftchinese.ui.channel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.content.ChannelSource
import com.ft.ftchinese.model.content.StoryBuilder
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.repository.Config
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.ui.base.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class ChannelViewModel(val cache: FileCache) :
        BaseViewModel(), AnkoLogger {

    val swipingLiveData: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    val htmlRendered: MutableLiveData<FetchResult<String>> by lazy {
        MutableLiveData<FetchResult<String>>()
    }

    fun load(channelSource: ChannelSource, account: Account?) {
        info("Loading channel content from $channelSource")

       val cacheName = channelSource.fileName

        info("Channel page cache file: $cacheName")

        viewModelScope.launch {
            // Fetch from server
            if (cacheName.isNullOrBlank()) {
                loadFromServer(channelSource, account)
                return@launch
            }

            val ok = loadFromCache(cacheName, account)
            // If loaded from cache, update cache silently and stop.
            if (ok) {
                silentUpdate(channelSource, account)
                return@launch
            }

            loadFromServer(channelSource, account)
        }
    }

    /**
     * @return Boolean - true if loaded from cache, false otherwise.
     */
    private suspend fun loadFromCache(cacheName: String, account: Account?): Boolean {
        progressLiveData.value = true
        try {
            info("Loading channel cache file $cacheName")
            val content = withContext(Dispatchers.IO) {
                cache.loadText(cacheName)
            }
            // Cache not found
            if (content.isNullOrBlank()) {
                info("Cached channel not found")
                progressLiveData.value = false
                return false
            }

            htmlRendered.value = render(content, account)
            progressLiveData.value = false
            // Rendered from cache
            return true
        } catch (e: Exception) {
            info(e)
            // Error from cache.
            progressLiveData.value = false
            return false
        }
    }

    private suspend fun loadFromServer(channelSource: ChannelSource, account: Account?) {
        progressLiveData.value = true

        htmlRendered.value = fetchAndRender(channelSource, account)

        progressLiveData.value = false
    }

    fun refresh(channelSource: ChannelSource, account: Account?) {
        viewModelScope.launch {
            swipingLiveData.value = true
            htmlRendered.value = fetchAndRender(channelSource, account)
            swipingLiveData.value = false
        }
    }

    private suspend fun render(content: String, account: Account?): FetchResult<String> {
        val template = withContext(Dispatchers.IO) {
            cache.readChannelTemplate()
        }

        val html = withContext(Dispatchers.Default) {
            StoryBuilder(template)
                .withChannel(content)
                .withUserInfo(account)
                .render()
        }

        return FetchResult.Success(html)
    }

    private suspend fun silentUpdate(channelSource: ChannelSource, account: Account?) {
        if (isNetworkAvailable.value != true) {
            return
        }

        val url = Config.buildChannelSourceUrl(account, channelSource) ?: return

        val content = withContext(Dispatchers.IO) {
            Fetch()
                .get(url.toString())
                .endPlainText()
        } ?: return

        render(content, account)

        channelSource.fileName?.let {
            cacheChannel(it, content)
        }
    }

    // Used when both loading and refreshing.
    private suspend fun fetchAndRender(channelSource: ChannelSource, account: Account?): FetchResult<String> {
        info("Fetching channel from server")
        if (isNetworkAvailable.value != true) {
            return FetchResult.LocalizedError(R.string.prompt_no_network)
        }

        val url = Config.buildChannelSourceUrl(account, channelSource)
        if (url == null) {
            info("Failed to build channel fetch url")
            return FetchResult.LocalizedError(R.string.api_empty_url)
        }

        try {
            val content = withContext(Dispatchers.IO) {
                Fetch()
                    .get(url.toString())
                    .endPlainText()
            }

            if (content.isNullOrBlank()) {
                info("Channel data not fetched from server")
                return FetchResult.LocalizedError(R.string.loading_failed)
            }

            channelSource.fileName?.let {
                cacheChannel(it, content)
            }

            info("Return render result...")
            return render(content, account)
        } catch (e: Exception) {
            info(e)
            return FetchResult.fromException(e)
        }
    }

    private fun cacheChannel(filename: String, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            info("Caching channel started $filename")
            cache.saveText(filename, content)
            info("Caching channel finished $filename")
        }
    }
}
