package com.ft.ftchinese.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.ui.components.ShowToast
import com.ft.ftchinese.ui.release.ReleaseActivity

@Composable
fun PreferenceActivityScreen(
    settingsViewModel: SettingsViewModel,
    onNavigateTo: (SettingScreen) -> Unit
) {
    val context = LocalContext.current
    val cacheSize by settingsViewModel.cacheSizeLiveData.observeAsState()
    val readCount by settingsViewModel.articlesReadLiveData.observeAsState()
    val toast by settingsViewModel.toastMessage.observeAsState()

    LaunchedEffect(key1 = Unit) {
        settingsViewModel.calculateCacheSize()
        settingsViewModel.countReadArticles()
    }

    ShowToast(
        toast = toast
    ) {
        settingsViewModel.clearToast()
    }

    PreferenceScreen(
        cacheSize = cacheSize,
        readCount = readCount,
        onClickRow = { rowId ->
            when (rowId) {
                SettingScreen.ClearCache -> {
                    settingsViewModel.clearCache()
                }
                SettingScreen.ClearHistory -> {
                    settingsViewModel.truncateReadArticles()
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
