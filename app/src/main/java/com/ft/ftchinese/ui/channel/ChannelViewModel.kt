package com.ft.ftchinese.ui.channel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.content.ChannelSource
import com.ft.ftchinese.model.content.TemplateBuilder
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.repository.Config
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.ui.base.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "ChannelViewModel"

class ChannelViewModel(val cache: FileCache) :
        BaseViewModel() {

    val swipingLiveData: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    val htmlRendered: MutableLiveData<FetchResult<String>> by lazy {
        MutableLiveData<FetchResult<String>>()
    }

    fun load(channelSource: ChannelSource, account: Account?) {

       val cacheName = channelSource.fileName

        Log.i(TAG, "Loading channel content for $channelSource. Cache file name $cacheName")

        viewModelScope.launch {
            // Fetch from server
            if (cacheName.isNullOrBlank()) {
                Log.i(TAG, "Channel ${channelSource.path} cache file name empty. Load from server.")
                loadFromServer(channelSource, account)
                return@launch
            }

            val ok = loadFromCache(cacheName, account)
            // If loaded from cache, update cache silently and stop.
            if (ok) {
                Log.i(TAG, "Channel ${channelSource.path} cache found. Start silent update.")
                silentUpdate(channelSource, account)
                return@launch
            }

            Log.i(TAG, "Start loading ${channelSource.path} from server")
            loadFromServer(channelSource, account)
        }
    }

    /**
     * @return Boolean - true if loaded from cache, false otherwise.
     */
    private suspend fun loadFromCache(cacheName: String, account: Account?): Boolean {
        progressLiveData.value = true
        try {
            Log.i(TAG, "Loading channel cache file $cacheName")
            val content = withContext(Dispatchers.IO) {
                cache.loadText(cacheName)
            }
            // Cache not found
            if (content.isNullOrBlank()) {
                Log.i(TAG, "Cached channel not found")
                progressLiveData.value = false
                return false
            }

            htmlRendered.value = render(content, account)
            progressLiveData.value = false
            // Rendered from cache
            return true
        } catch (e: Exception) {
            e.message?.let {
                Log.i(TAG, it)
            }
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
            TemplateBuilder(template)
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

        Log.i(TAG, "Silent update ${channelSource.path} from $url")

        try {
            val content = withContext(Dispatchers.IO) {
                Fetch()
                    .get(url.toString())
                    .endPlainText()
            } ?: return

            render(content, account)

            channelSource.fileName?.let {
                cacheChannel(it, content)
            }
        } catch (e: Exception) {
            e.message?.let {
                Log.i(TAG, it)
            }
        }
    }

    // Used when both loading and refreshing.
    private suspend fun fetchAndRender(channelSource: ChannelSource, account: Account?): FetchResult<String> {
        Log.i(TAG, "Fetching channel ${channelSource.path} from server")
        if (isNetworkAvailable.value != true) {
            return FetchResult.LocalizedError(R.string.prompt_no_network)
        }

        val url = Config.buildChannelSourceUrl(account, channelSource)
        if (url == null) {
            Log.i(TAG,"Channel url for ${channelSource.path} is emmpty")
            return FetchResult.LocalizedError(R.string.api_empty_url)
        }

        Log.i(TAG, "Channel ${channelSource.path} url $url")
        try {
            val content = withContext(Dispatchers.IO) {
                Fetch()
                    .get(url.toString())
                    .endPlainText()
            }

            if (content.isNullOrBlank()) {
                Log.i(TAG, "Channel ${channelSource.path} data not fetched from server")
                return FetchResult.LocalizedError(R.string.loading_failed)
            }

            channelSource.fileName?.let {
                cacheChannel(it, content)
            }

            return render(content, account)
        } catch (e: Exception) {
            e.message?.let{ Log.i(TAG, it)}
            return FetchResult.fromException(e)
        }
    }

    private fun cacheChannel(filename: String, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.i(TAG, "Caching channel started $filename")
            cache.saveText(filename, content)
        }
    }
}
