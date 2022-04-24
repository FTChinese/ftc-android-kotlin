package com.ft.ftchinese.ui.channel

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ft.ftchinese.model.content.ChannelSource
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.repository.Config
import com.ft.ftchinese.service.*
import com.ft.ftchinese.ui.components.ProgressLayout
import com.ft.ftchinese.ui.components.ToastMessage
import com.ft.ftchinese.ui.components.WebPage
import com.ft.ftchinese.ui.webpage.BaseJsEventListener
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.accompanist.web.rememberWebViewStateWithHTMLData
import java.util.*

@Composable
fun ChannelFragmentScreen(
    account: Account?,
    source: ChannelSource?,
    channelViewModel: ChannelViewModel,
    showSnackBar: (String) -> Unit,
) {
    val context = LocalContext.current
    val baseUrl by remember {
        mutableStateOf(Config.discoverServer(account))
    }
    val startTime by remember {
        mutableStateOf(Date().time / 1000)
    }
    val loading by channelViewModel.progressLiveData.observeAsState(false)
    val isRefreshing by channelViewModel.refreshingLiveData.observeAsState(false)
    val htmlData by channelViewModel.htmlLiveData.observeAsState("")
    val err by channelViewModel.errorLiveData.observeAsState()

    val wvState = rememberWebViewStateWithHTMLData(
        data = htmlData,
        baseUrl = if (htmlData.isBlank()) {
            null
        } else {
            baseUrl
        }
    )

    if (source == null) {
        return
    }

    LaunchedEffect(key1 = Unit) {
        channelViewModel.load(source, account)
    }

    DisposableEffect(key1 = Unit) {
        onDispose {
            if (account == null) {
                return@onDispose
            }

            sendReadLen(
                context = context,
                userId = account.id,
                startTime = startTime,
                source = source
            )
        }
    }

    when (val m = err) {
        is ToastMessage.Resource -> {
            showSnackBar(context.getString(m.id))
        }
        is ToastMessage.Text -> {
            showSnackBar(m.text)
        }
        else -> {}
    }

    ProgressLayout(
        loading = loading
    ) {
        SwipeRefresh(
            state = rememberSwipeRefreshState(
                isRefreshing = isRefreshing
            ),
            onRefresh = {
                channelViewModel.refresh(source, account)
            }
        ) {
            WebPage(
                wvState = wvState,
                jsEventListener = BaseJsEventListener(context),
            )
        }
    }
}

private fun sendReadLen(
    context: Context,
    userId: String,
    startTime: Long,
    source: ChannelSource,
) {
    val data: Data = workDataOf(
        KEY_DUR_URL to "/android/channel/${source.title}",
        KEY_DUR_REFER to "http://www.ftchinese.com/",
        KEY_DUR_START to startTime,
        KEY_DUR_END to Date().time / 1000,
        KEY_DUR_USER_ID to userId
    )

    val lenWorker = OneTimeWorkRequestBuilder<ReadingDurationWorker>()
        .setInputData(data)
        .build()

    context.run {
        WorkManager.getInstance(this).enqueue(lenWorker)
    }
}
