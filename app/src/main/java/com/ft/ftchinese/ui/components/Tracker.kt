package com.ft.ftchinese.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ft.ftchinese.model.content.ChannelSource
import com.ft.ftchinese.model.content.Teaser
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.repository.HostConfig
import com.ft.ftchinese.service.*
import java.util.*

@Composable
fun rememberStartTime() = remember {
    Date().time / 1000
}

fun sendChannelReadLen(
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

    WorkManager.getInstance(context).enqueue(lenWorker)
}

fun sendArticleReadLen(
    context: Context,
    account: Account,
    teaser: Teaser,
    startAt: Long
) {
    val data: Data = workDataOf(
        KEY_DUR_URL to "/android/${teaser.type}/${teaser.id}/${teaser.title}",
        KEY_DUR_REFER to HostConfig.discoverServer(account),
        KEY_DUR_START to startAt,
        KEY_DUR_END to Date().time / 1000,
        KEY_DUR_USER_ID to account.id
    )

    val lenWorker = OneTimeWorkRequestBuilder<ReadingDurationWorker>()
        .setInputData(data)
        .build()

    WorkManager.getInstance(context).enqueue(lenWorker)
}
