package com.ft.ftchinese.ui.main.home

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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ft.ftchinese.model.content.ChannelSource
import com.ft.ftchinese.ui.article.chl.rememberChannelState
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.components.rememberBaseUrl
import com.ft.ftchinese.ui.web.FtcWebView
import com.ft.ftchinese.ui.web.JsSnippets
import com.ft.ftchinese.ui.web.WebViewCallback
import com.ft.ftchinese.ui.web.rememberFtcJsEventListener
import com.ft.ftchinese.viewmodel.UserViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.accompanist.web.rememberWebViewStateWithHTMLData

private const val TAG = "ChannelTab"

@Composable
fun ChannelTabScreen(
    userViewModel: UserViewModel = viewModel(),
    scaffoldState: ScaffoldState,
    channelSource: ChannelSource,
) {
    val context = LocalContext.current
    val accountState = userViewModel.accountLiveData.observeAsState()

    val baseUrl = rememberBaseUrl(account = accountState.value)

    val channelState = rememberChannelState(
        scaffoldState = scaffoldState,
    )

    LaunchedEffect(key1 = Unit) {
        channelState.setTabbedChannelSource(channelSource)
    }

    val wvState = rememberWebViewStateWithHTMLData(
        data = channelState.htmlLoaded,
        baseUrl = baseUrl
    )

    val jsCallback = rememberFtcJsEventListener(
        channelSource = channelSource
    )

    val wvCallback = remember(accountState.value) {
        object : WebViewCallback(context) {
            override fun onPageFinished(view: WebView?, url: String?) {
                view?.evaluateJavascript(
                    JsSnippets.lockerIcon(userViewModel.account?.membership?.tier)
                ) {
                    Log.i(TAG, "Privilege result: $it")
                }
                super.onPageFinished(view, url)
            }
        }
    }

    LaunchedEffect(key1 = baseUrl, channelState.channelSource) {
        channelState.initLoading(
            baseUrl = baseUrl,
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
        modifier = Modifier.fillMaxSize(),
    ) {
        SwipeRefresh(
            state = rememberSwipeRefreshState(
                isRefreshing = channelState.refreshing
            ),
            onRefresh = {
                channelState.refresh(
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
                FtcWebView(
                    wvState = wvState,
                    webClientCallback = wvCallback,
                    jsListener = jsCallback,
                )
            }
        }
    }
}
