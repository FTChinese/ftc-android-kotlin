package com.ft.ftchinese.ui.article.content

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.model.content.Language
import com.ft.ftchinese.ui.components.ButtonGroupLayout
import com.ft.ftchinese.ui.components.OButtonDefaults
import com.ft.ftchinese.ui.components.SelectButton

@Composable
fun LanguageSwitch(
    modifier: Modifier = Modifier,
    currentLang: Language,
    onSelect: (Language) -> Unit
) {
    ButtonGroupLayout(
        modifier = modifier
    ) {
        listOf(
            Language.CHINESE,
            Language.ENGLISH,
            Language.BILINGUAL
        ).forEach { lang ->
            val selected = currentLang == lang

            SelectButton(
                selected = selected,
                onSelect = { onSelect(lang) },
                colors = OButtonDefaults.selectButtonColors()
            ) {
                Text(
                    text = stringResource(id = lang.nameId),
                    style = MaterialTheme.typography.subtitle2,
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
