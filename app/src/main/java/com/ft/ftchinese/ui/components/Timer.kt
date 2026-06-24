package com.ft.ftchinese.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.ft.ftchinese.R
import kotlinx.coroutines.*

const val MOBILE_SMS_CODE_TOTAL_TIME_SECONDS = 15L * 60L

class TimerState(
    private val totalTime: Long,
    private val scope: CoroutineScope,
    private val initialText: String = "",
) {
    var currentTime by mutableStateOf(totalTime)
        private set

    var isRunning by mutableStateOf(false)
        private set

    private var job: Job? = null

    val text = derivedStateOf {
        if (!isRunning) {
            initialText
        } else {
            formatRemainingTime(currentTime)
        }
    }

    fun start() {
        isRunning = true
        currentTime = totalTime
        job = scope.launch(Dispatchers.Main) {
            for (i in totalTime downTo 0) {
                currentTime = i
                delay(1000)
            }
            isRunning = false
            currentTime = totalTime
        }
    }

    fun stop() {
        job?.cancel()
    }
}

private fun formatRemainingTime(seconds: Long): String {
    if (seconds < 60) {
        return "${seconds}s"
    }

    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "${minutes}:${remainingSeconds.toString().padStart(2, '0')}"
}

@Composable
fun rememberTimerState(
    totalTime: Long = 60,
    initialText: String = stringResource(id = R.string.mobile_request_code),
    scope: CoroutineScope = rememberCoroutineScope()
) = remember(totalTime, initialText, scope) {
    TimerState(
        totalTime = totalTime,
        initialText = initialText,
        scope = scope,
    )
}
