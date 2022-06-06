package com.ft.ftchinese.ui.settings.overview

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.ui.settings.SettingScreen
import com.ft.ftchinese.ui.settings.release.ReleaseActivity

@Composable
fun PreferenceActivityScreen(
    scaffoldState: ScaffoldState,
    onNavigateTo: (SettingScreen) -> Unit
) {
    val context = LocalContext.current
    val prefState = rememberPrefState(
        scaffoldState = scaffoldState
    )

    LaunchedEffect(key1 = Unit) {
        prefState.calculateCacheSize()
        prefState.countReadArticles()
    }

    PreferenceScreen(
        cacheSize = prefState.cacheSize,
        readCount = prefState.readCount,
        onClickRow = { rowId ->
            when (rowId) {
                SettingScreen.ClearCache -> {
                    prefState.clearCache()
                }
                SettingScreen.ClearHistory -> {
                    prefState.truncateReadArticles()
                }
                SettingScreen.CheckVersion -> {
                    ReleaseActivity.start(context)
                }
                else -> {
                    onNavigateTo(rowId)
                }
            }
        }
    )
}
