package com.ft.ftchinese.ui.article.chl

import android.util.Log
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.model.content.ChannelSource
import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.ui.article.NavStore
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.components.rememberBaseUrl
import com.ft.ftchinese.ui.web.*
import com.ft.ftchinese.viewmodel.UserViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.accompanist.web.rememberWebViewStateWithHTMLData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "ChannelActivity"

@Composable
fun ChannelActivityScreen(
    userViewModel: UserViewModel = viewModel(),
    scaffoldState: ScaffoldState,
    id: String?,
    onArticle: (id: String) -> Unit,
    onChannel: (id: String) -> Unit,
) {
    val context = LocalContext.current
    val account by userViewModel.accountLiveData.observeAsState()
    val baseUrl = rememberBaseUrl(account)
    val scope = rememberCoroutineScope()

    val channelState = rememberChannelState(
        scaffoldState = scaffoldState,
    )

    LaunchedEffect(key1 = Unit) {
        channelState.findChannelSource(id)
    }

    val wvState = rememberWebViewStateWithHTMLData(
        data = channelState.htmlLoaded,
        baseUrl = baseUrl
    )

    val jsCallback = remember(channelState.channelSource) {
        object : FtcJsEventListener(context, channelState.channelSource) {
            override fun onClickTeaser(teaser: Teaser) {
                Log.i(TAG, "Clicked a teaser item $teaser")
                val t = teaser.withParentPerm(channelState.channelSource?.permission)
                scope.launch(Dispatchers.Main) {
                    onArticle(NavStore.saveTeaser(t))
                }
            }

            override fun onClickChannel(source: ChannelSource) {
                Log.i(TAG, "Clicked a channel item $source")
                scope.launch(Dispatchers.Main) {
                    onChannel(NavStore.saveChannel(source))
                }
            }
        }
    }

    val wvCallback = remember(
        channelState.channelSource,
    ) {
        object : WebViewCallback(
            context,
            channelSource = channelState.channelSource
        ) {
            override fun onPageFinished(view: WebView?, url: String?) {
                view?.evaluateJavascript(
                    JsSnippets.lockerIcon(userViewModel.account?.membership?.tier)
                ) {
                    Log.i(TAG, "Privilege result: $it")
                }
                super.onPageFinished(view, url)
            }

            override fun onClickChannel(source: ChannelSource) {
                Log.i(TAG, "Clicked a channel link $source")
                onChannel(NavStore.saveChannel(
                    source.withParentPerm(
                        channelState.channelSource?.permission
                    )
                ))
            }

            override fun onClickStory(teaser: Teaser) {
                Log.i(TAG, "Clicked a story link $teaser")
                onArticle(NavStore.saveTeaser(teaser))
            }
        }
    }

    LaunchedEffect(
        key1 = account,
        key2 = channelState.channelSource
    ) {
        channelState.initLoading(
            account = userViewModel.account
        )
    }

    DisposableEffect(key1 = Unit) {
        onDispose {
            userViewModel.account?.id?.let {
                channelState.sendReadDur(
                    context = context,
                    userId = it
                )
            }
        }
    }

    ProgressLayout(
        loading = channelState.progress.value,
        modifier = Modifier.fillMaxSize()
    ) {
        SwipeRefresh(
            state = rememberSwipeRefreshState(
                isRefreshing = channelState.refreshing
            ),
            onRefresh = {
                channelState.refresh(
                    account = userViewModel.account
                )
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                FtcWebView(
                    wvState = wvState,
                    webClientCallback = wvCallback,
                    jsListener = jsCallback,
                )
            }
        }
    }
}

