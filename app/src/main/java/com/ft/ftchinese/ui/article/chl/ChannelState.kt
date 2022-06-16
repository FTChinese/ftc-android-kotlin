package com.ft.ftchinese.ui.article.chl

import android.content.Context
import android.util.Log
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.model.content.ChannelSource
import com.ft.ftchinese.model.content.TemplateBuilder
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.repository.ArticleClient
import com.ft.ftchinese.store.FileStore
import com.ft.ftchinese.ui.article.NavStore
import com.ft.ftchinese.ui.base.ConnectionState
import com.ft.ftchinese.ui.base.connectivityState
import com.ft.ftchinese.ui.components.BaseState
import com.ft.ftchinese.ui.components.sendChannelReadLen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

private const val TAG = "ChannelState"

data class Loaded(
    val html: String,
    val isRemote: Boolean
)

class ChannelState(
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
    connState: State<ConnectionState>,
    context: Context
) : BaseState(scaffoldState, scope, context.resources, connState) {
    private val cache = FileStore(context)
    private val startTime = Date().time / 1000

    var refreshing by mutableStateOf(false)
        private set

    var htmlLoaded by mutableStateOf("")
        private set

    var channelSource by mutableStateOf<ChannelSource?>(null)
            private set

    fun findChannelSource(id: String?) {
        if (id == null) {
            showSnackBar("Missing id")
            return
        }

        channelSource = NavStore.getChannel(id)
    }

    fun setTabbedChannelSource(source: ChannelSource) {
        channelSource = source
    }

    fun initLoading(
        baseUrl: String,
        account: Account?
    ) {
        val source = channelSource ?: return
        progress.value = true
        scope.launch {
            val result = retrieveHtml(
                source,
                baseUrl,
                refresh = false
            )

            when (result) {
                is FetchResult.LocalizedError -> {
                    progress.value = false
                    showSnackBar(result.msgId)
                }
                is FetchResult.TextError -> {
                    progress.value = false
                    showSnackBar(result.text)
                }
                is FetchResult.Success -> {
                    onChannelLoaded(
                        content = result.data.html,
                        isFragment = source.isFragment,
                        account = account,
                    )
                    progress.value = false

                    if (!result.data.isRemote) {
                        silentUpdate(
                            source = source,
                            baseUrl = baseUrl,
                            account = account,
                        )
                    }
                }
            }
        }
    }

    private suspend fun silentUpdate(
        source: ChannelSource,
        baseUrl: String,
        account: Account?
    ) {
        val result = retrieveHtml(
            source,
            baseUrl,
            refresh = true
        )

        when (result) {
            is FetchResult.Success -> {
                onChannelLoaded(
                    content = result.data.html,
                    isFragment = source.isFragment,
                    account = account,
                )
            }
            else -> {

            }
        }
    }

    fun refresh(
        baseUrl: String,
        account: Account?
    ) {
        val source = channelSource ?: return
        refreshing = true
        scope.launch {
            val result = retrieveHtml(
                source,
                baseUrl,
                refresh = true
            )

            when (result) {
                is FetchResult.LocalizedError -> {
                    showSnackBar(result.msgId)
                }
                is FetchResult.TextError -> {
                    showSnackBar(result.text)
                }
                is FetchResult.Success -> {
                    onChannelLoaded(
                        content = result.data.html,
                        isFragment = source.isFragment,
                        account = account,
                    )

                    showRefreshed()
                }
            }

            refreshing = false
        }
    }

    private suspend fun onChannelLoaded(
        content: String,
        isFragment: Boolean,
        account: Account?
    ) {
        htmlLoaded = if (isFragment) {
            render(content, account)
        } else {
            content
        }
    }

    private suspend fun retrieveHtml(
        source: ChannelSource,
        baseUrl: String,
        refresh: Boolean
    ): FetchResult<Loaded> {
        val cacheName = source.fileName

        if (!refresh && !cacheName.isNullOrBlank()) {
            Log.i(TAG, "Load channel ${source.path} from cache")
            val html = cache.asyncLoadText(cacheName)
            if (html != null) {
                return FetchResult.Success(
                    Loaded(
                    html = html,
                    isRemote = false
                )
                )
            }
        }

        val url = source.htmlUrl(baseUrl)
        Log.i(TAG,"Channel url for ${source.path} is empty")

        if (url.isNullOrBlank()) {
            return FetchResult.TextError("Empty url to load")
        }

        if (!isConnected) {
            return FetchResult.notConnected
        }

        when (val result = ArticleClient.asyncCrawlFile(url)) {
            is FetchResult.LocalizedError -> {
                return result
            }
            is FetchResult.TextError -> {
                return result
            }
            is FetchResult.Success -> {
                scope.launch(Dispatchers.IO) {
                    Log.i(TAG, "Cache file $cacheName")
                    cacheName?.let {
                        cache.saveText(it, result.data)
                    }

                }

                return FetchResult.Success(
                    Loaded(
                    html = result.data,
                    isRemote = true
                )
                )
            }
        }
    }

    private suspend fun render(
        content: String,
        account: Account?
    ): String {
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

    fun sendReadDur(
        context: Context,
        userId: String
    ) {
        channelSource?.let {
            sendChannelReadLen(
                context = context,
                userId = userId,
                startTime = startTime,
                source = it
            )
        }
    }
}


@Composable
fun rememberChannelState(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    connState: State<ConnectionState> = connectivityState(),
    scope: CoroutineScope = rememberCoroutineScope(),
    context: Context = LocalContext.current,
) = remember(scaffoldState, connState) {
    ChannelState(
        scaffoldState = scaffoldState,
        scope = scope,
        connState = connState,
        context = context
    )
}
