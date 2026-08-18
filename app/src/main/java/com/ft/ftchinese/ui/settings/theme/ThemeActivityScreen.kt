package com.ft.ftchinese.ui.settings.theme

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ft.ftchinese.model.settings.AppTheme
import com.ft.ftchinese.store.AppThemeManager
import com.ft.ftchinese.store.SettingStore
import com.ft.ftchinese.ui.components.CheckVariant
import com.ft.ftchinese.ui.components.OCheckbox
import com.ft.ftchinese.ui.components.SelectableRow

private class ThemeSettingState(context: Context) {
    private val settings = SettingStore.getInstance(context)

    var selected by mutableStateOf(AppThemeManager.current(context))
        private set

    fun select(context: Context, theme: AppTheme) {
        if (selected == theme) return
        selected = theme
        settings.saveAppTheme(theme)
        AppThemeManager.apply(context)
    }
}

@Composable
private fun rememberThemeSettingState(
    context: Context = LocalContext.current,
): ThemeSettingState = remember(context) { ThemeSettingState(context) }

@Composable
fun ThemeActivityScreen() {
    val context = LocalContext.current
    val state = rememberThemeSettingState(context)

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
        ) {
            AppTheme.values().forEach { theme ->
                val selected = state.selected == theme
                SelectableRow(
                    selected = selected,
                    onSelect = { state.select(context, theme) },
                    endIcon = {
                        OCheckbox(
                            checked = selected,
                            onCheckedChange = { state.select(context, theme) },
                            variant = CheckVariant.Square,
                        )
                    },
                ) {
                    Text(
                        text = stringResource(theme.labelId),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
