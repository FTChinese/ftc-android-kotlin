package com.ft.ftchinese.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.ft.ftchinese.R
import kotlinx.coroutines.*

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
            "${currentTime}s"
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
