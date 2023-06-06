package com.ft.ftchinese.ui.settings.fontsize

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.model.enums.FontSize
import com.ft.ftchinese.store.SettingStore

class SettingState(
    context: Context
) {
    private val settings = SettingStore.getInstance(context)

    var currentFontSize by mutableStateOf(settings.loadFontSize())
        private set

    fun onSelect(fs: FontSize) {
        currentFontSize = fs
        settings.saveFontSize(fs)
    }
}

@Composable
fun rememberSettingState(
    context: Context = LocalContext.current
) = remember {
    SettingState(context)
}
