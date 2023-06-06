package com.ft.ftchinese.ui.settings.fontsize

import androidx.compose.runtime.Composable

@Composable
fun FontSizeActivityScreen() {
    val settingState = rememberSettingState()

    FontSizeScreen(
        selected = settingState.currentFontSize,
        onSelect = {
            settingState.onSelect(it)
        }
    )
}
