package com.ft.ftchinese.ui.article

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.model.content.Language
import com.ft.ftchinese.ui.components.ButtonGroupLayout
import com.ft.ftchinese.ui.components.OButtonDefaults
import com.ft.ftchinese.ui.components.SelectButton
import com.ft.ftchinese.ui.components.SubHeading2

@Composable
fun LanguageSwitch(
    currentLang: Language,
    onSelect: (Language) -> Unit
) {
    ButtonGroupLayout {
        listOf(
            Language.CHINESE,
            Language.ENGLISH,
            Language.BILINGUAL
        ).forEach { lang ->
            val selected = currentLang == lang

            SelectButton(
                selected = selected,
                onSelect = { onSelect(lang) },
                colors = if (selected) {
                    OButtonDefaults.buttonColors()
                } else {
                    OButtonDefaults.textButtonColors()
                }
            ) {
                SubHeading2(
                    text = stringResource(id = lang.nameId)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewLanguageSwitch() {
    LanguageSwitch(
        currentLang = Language.CHINESE,
        onSelect = {}
    )
}