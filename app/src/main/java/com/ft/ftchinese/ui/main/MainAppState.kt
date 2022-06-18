package com.ft.ftchinese.ui.main

import android.content.Context
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.ui.components.BaseState
import com.ft.ftchinese.ui.util.ConnectionState
import com.ft.ftchinese.ui.util.connectivityState
import kotlinx.coroutines.CoroutineScope

class MainAppState(
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
    connState: State<ConnectionState>,
    context: Context
) : BaseState(scaffoldState, scope, context.resources, connState)  {
    val tracker = StatsTracker.getInstance(context)

    fun trackAppOpened() {
        tracker.appOpened()
    }

    fun trackChannelSelected(title: String) {
        tracker.tabSelected(title)
    }
}

@Composable
fun rememberMainAppState(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    connState: State<ConnectionState> = connectivityState(),
    scope: CoroutineScope = rememberCoroutineScope(),
    context: Context = LocalContext.current,
) = remember(scaffoldState, connState) {
    MainAppState(
        scaffoldState = scaffoldState,
        scope = scope,
        connState = connState,
        context = context
    )
}
