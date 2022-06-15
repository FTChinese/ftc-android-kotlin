package com.ft.ftchinese.ui.channel

import android.util.Log
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.model.content.ChannelSource
import com.ft.ftchinese.repository.Config
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.components.rememberStartTime
import com.ft.ftchinese.ui.components.sendChannelReadLen
import com.ft.ftchinese.ui.web.*
import com.ft.ftchinese.viewmodel.UserViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.accompanist.web.rememberWebViewStateWithHTMLData

private const val TAG = "ChannelActivity"

@Composable
fun ChannelActivityScreen(
    userViewModel: UserViewModel = viewModel(),
    scaffoldState: ScaffoldState,
    channelSource: ChannelSource,
) {
    val context = LocalContext.current
    val baseUrl = remember(userViewModel.account) {
        Config.discoverServer(userViewModel.account)
    }
    val startTime = rememberStartTime()

    val channelState = rememberChannelState(
        scaffoldState = scaffoldState,
    )

    val wvState = rememberWebViewStateWithHTMLData(
        data = channelState.htmlLoaded,
        baseUrl = baseUrl
    )

    val wvCallback = remember(userViewModel.account) {
        object : WebViewCallback(context, userViewModel.account) {
            override fun onPageFinished(view: WebView?, url: String?) {
                view?.evaluateJavascript(
                    JsSnippets.lockerIcon(userViewModel.account?.membership?.tier)
                ) {
                    Log.i(TAG, "Privilege result: $it")
                }
                super.onPageFinished(view, url)
            }

            override fun onPagination(paging: Paging) {
                val pagedSource = channelSource.withPagination(
                    paging.key, paging.page
                )

                if (channelSource.isSamePage(pagedSource)) {
                    return
                }

                ChannelActivity.start(context, pagedSource)
            }

            override fun onChannelSelected(source: ChannelSource) {
                ChannelActivity.start(
                    context,
                    source.withParentPerm(
                        channelSource.permission
                    )
                )
            }
        }
    }
    val webClient = rememberWebViewClient(
        callback = wvCallback
    )

    LaunchedEffect(key1 = baseUrl) {
        channelState.initLoading(
            source = channelSource,
            baseUrl = baseUrl,
            account = userViewModel.account
        )
    }

    DisposableEffect(key1 = Unit) {
        onDispose {
            val account = userViewModel.account ?: return@onDispose

            sendChannelReadLen(
                context = context,
                userId = account.id,
                startTime = startTime,
                source = channelSource
            )
        }
    }

    ProgressLayout(
        loading = channelState.progress.value
    ) {
        SwipeRefresh(
            state = rememberSwipeRefreshState(
                isRefreshing = channelState.refreshing
            ),
            onRefresh = {
                channelState.refresh(
                    source = channelSource,
                    baseUrl = baseUrl,
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
                ComposeWebView(
                    wvState = wvState,
                    webClient = webClient
                )
            }
        }
    }
}

