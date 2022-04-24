package com.ft.ftchinese.ui.channel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.R
import com.ft.ftchinese.model.content.ChannelSource
import com.ft.ftchinese.model.content.TemplateBuilder
import com.ft.ftchinese.model.fetch.Fetch
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.repository.Config
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.ui.components.ToastMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "ChannelViewModel"

class ChannelViewModel(application: Application) :
        AndroidViewModel(application) {

    private val cache: FileCache = FileCache(application)
    val progressLiveData = MutableLiveData<Boolean>()
    val isNetworkAvailable = MutableLiveData(application.isConnected)

    val refreshingLiveData: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    val errorLiveData: MutableLiveData<ToastMessage> by lazy {
        MutableLiveData<ToastMessage>()
    }

    val htmlLiveData: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    fun load(channelSource: ChannelSource, account: Account?) {

        progressLiveData.value = true
        viewModelScope.launch {

            val loaded = loadFromCacheOrRemote(channelSource, account)

            if (loaded == null) {
                errorLiveData.value = ToastMessage.Text("Error: no data loaded!")
                progressLiveData.value = false
                return@launch
            }

            setHtmlData(
                isFragment = channelSource.isFragment,
                htmlData = loaded.html,
                account = account
            )

            progressLiveData.value = false

            if (loaded.isRemote || isNetworkAvailable.value != true) {
                return@launch
            }

            Log.i(TAG, "Background update ${channelSource.path}")
            loadRemoteHtml(channelSource, account)?.let {
                setHtmlData(
                    isFragment = channelSource.isFragment,
                    htmlData = it,
                    account = account
                )
            }
        }
    }

    private suspend fun loadFromCacheOrRemote(source: ChannelSource, account: Account?): Loaded? {
        val cacheName = source.fileName

        if (!cacheName.isNullOrBlank()) {
            Log.i(TAG, "Load channel ${source.path} from cache")
            val html = loadCachedHtml(cacheName)
            if (html != null) {
                return Loaded(
                    html = html,
                    isRemote = false
                )
            }
        }

        if (isNetworkAvailable.value != true) {
            errorLiveData.value = ToastMessage.Resource(R.string.prompt_no_network)
            return null
        }

        Log.i(TAG, "Load channel ${source.path} from server")
        return loadRemoteHtml(source, account)?.let {
            Loaded(
                html = it,
                isRemote = true
            )
        }
    }

    fun refresh(channelSource: ChannelSource, account: Account?) {
        errorLiveData.value = ToastMessage.Resource(R.string.refreshing_data)
        refreshingLiveData.value = true

        if (isNetworkAvailable.value != true) {
            errorLiveData.value = ToastMessage.Resource(R.string.prompt_no_network)
            return
        }

        viewModelScope.launch {
            loadRemoteHtml(channelSource, account)?.let {
                setHtmlData(
                    isFragment = channelSource.isFragment,
                    htmlData = it,
                    account = account
                )
            }

            refreshingLiveData.value = false
        }
    }

    private suspend fun setHtmlData(
        isFragment: Boolean,
        htmlData: String,
        account: Account?
    ) {
        Log.i(TAG, "Set html string")
        htmlLiveData.value = if (isFragment) {
            render(htmlData, account)
        } else {
            htmlData
        }
    }

    private suspend fun loadCachedHtml(fileName: String): String? {
        return withContext(Dispatchers.IO) {
            cache.loadText(fileName)
        }
    }

    private suspend fun loadRemoteHtml(
        source: ChannelSource,
        account: Account?
    ): String? {

        val url = Config.buildChannelSourceUrl(account, source)
        if (url == null) {
            Log.i(TAG,"Channel url for ${source.path} is empty")
            errorLiveData.value = ToastMessage.Resource(R.string.api_empty_url)
            return null
        }

        Log.i(TAG, "Fetch channel page ${source.path} from $url")
        try {
            val content = withContext(Dispatchers.IO) {
                Fetch()
                    .get(url.toString())
                    .endText()
                    .body
            }

            if (content.isNullOrBlank()) {
                Log.i(TAG, "Channel ${source.path} data not fetched from server")
                errorLiveData.value = ToastMessage.Resource(R.string.loading_failed)
                return null
            }

            viewModelScope.launch(Dispatchers.IO) {

                source.fileName?.let {
                    Log.i(TAG, "Cache channel file $it")
                    cache.saveText(it, content)
                }
            }

            return content
        } catch (e: Exception) {
            e.message?.let { Log.i(TAG, it) }
            errorLiveData.value = ToastMessage.fromException(e)
            return null
        }
    }

    private suspend fun render(content: String, account: Account?): String {
        val template = withContext(Dispatchers.IO) {
            cache.readChannelTemplate()
        }

        return withContext(Dispatchers.Default) {
            TemplateBuilder(template)
                .withChannel(content)
                .withUserInfo(account)
                .render()
        }
    }
}
