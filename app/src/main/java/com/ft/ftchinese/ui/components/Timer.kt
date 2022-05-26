package com.ft.ftchinese.ui.components

import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.ft.ftchinese.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun Timer(
    totalTime: Long,
    isRunning: Boolean,
    initialText: String,
    onFinish: () -> Unit,
) {
    var currentTime by remember {
        mutableStateOf(totalTime)
    }
    
    LaunchedEffect(key1 = currentTime, key2 = isRunning) {
        if (isRunning) {
            if (currentTime > 0) {
                delay(1000L)
                currentTime -= 1
            } else {
                onFinish()
            }
        } else {
            currentTime = totalTime
        }
    }

    Text(
        text = if (isRunning) {
            "${currentTime}s"
        } else {
            initialText
        }
    )
}

class TimerState(
    private val totalTime: Long,
    private val initialText: String,
    private val scope: CoroutineScope,
) {
    var currentTime by mutableStateOf(totalTime)
        private set

    var isRunning by mutableStateOf(false)

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
        scope.launch(Dispatchers.Main) {
            for (i in totalTime downTo 0) {
                currentTime = i
                delay(1000)
            }
            isRunning = false
            currentTime = totalTime
        }
    }
}

@Composable
fun rememberTimerState(
    totalTime: Long = 60,
    initialText: String = stringResource(id = R.string.mobile_request_code),
    scope: CoroutineScope = rememberCoroutineScope()
) = remember(totalTime, initialText, scope) {
    TimerState(
        totalTime,
        initialText,
        scope,
    )
}
