package com.ft.ftchinese.ui.settings.overview

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.util.IntentsUtil
import com.ft.ftchinese.ui.util.toast

@Composable
fun PreferenceActivityScreen(
    scaffoldState: ScaffoldState,
    onNavigateTo: (SettingScreen) -> Unit
) {
    val context = LocalContext.current

    val scope = rememberCoroutineScope()

    val prefState = rememberPrefState(
        scaffoldState = scaffoldState,
        scope = scope,
    )

    LaunchedEffect(key1 = Unit) {
        prefState.calculateCacheSize()
        prefState.countReadArticles()
    }

    val rows = remember(
        context.resources,
        prefState.cacheSize,
        prefState.readEntries
    ) {
        SettingRow.build(
            context.resources,
            cacheSize = prefState.cacheSize,
            readCount = prefState.readEntries
        )
    }

    PreferenceScreen(
        rows = rows,
    ) { rowId ->
        when (rowId) {
            SettingScreen.ClearCache -> {
                prefState.clearCache()
            }
            SettingScreen.ClearHistory -> {
                prefState.truncateReadArticles()
            }
            SettingScreen.Feedback -> {
                val ok = IntentsUtil.sendFeedbackEmail(context = context)
                if (!ok) {
                    context.toast(R.string.prompt_no_email_app)
                }
            }
            else -> {
                onNavigateTo(rowId)
            }
        }
    }
}
