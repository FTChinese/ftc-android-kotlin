package com.ft.ftchinese.ui.settings.language

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
import com.ft.ftchinese.model.settings.AppLanguage
import com.ft.ftchinese.store.AppLanguageManager
import com.ft.ftchinese.store.SettingStore
import com.ft.ftchinese.ui.components.CheckVariant
import com.ft.ftchinese.ui.components.OCheckbox
import com.ft.ftchinese.ui.components.SelectableRow

private class LanguageSettingState(context: Context) {
    private val settings = SettingStore.getInstance(context)

    var selected by mutableStateOf(AppLanguageManager.current(context))
        private set

    fun select(context: Context, language: AppLanguage) {
        if (selected == language) {
            return
        }
        selected = language
        settings.saveAppLanguage(language)
        AppLanguageManager.apply(context)
    }
}

@Composable
private fun rememberLanguageSettingState(
    context: Context = LocalContext.current,
): LanguageSettingState = remember(context) {
    LanguageSettingState(context)
}

@Composable
fun LanguageActivityScreen() {
    val context = LocalContext.current
    val state = rememberLanguageSettingState(context)

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
        ) {
            AppLanguage.values().forEach { language ->
                val selected = state.selected == language
                SelectableRow(
                    selected = selected,
                    onSelect = { state.select(context, language) },
                    endIcon = {
                        OCheckbox(
                            checked = selected,
                            onCheckedChange = { state.select(context, language) },
                            variant = CheckVariant.Square,
                        )
                    },
                ) {
                    Text(
                        text = stringResource(language.labelId),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
