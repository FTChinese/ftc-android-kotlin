package com.ft.ftchinese.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ft.ftchinese.model.paywall.paywallGuide
import com.ft.ftchinese.model.paywall.paywallGuideTraditional
import com.ft.ftchinese.model.settings.AppLanguage
import com.ft.ftchinese.store.AppLanguageManager
import com.ft.ftchinese.ui.theme.OColor
import androidx.compose.ui.platform.LocalContext

@Composable
fun SubsRuleContent() {
    val context = LocalContext.current
    val guide = if (AppLanguageManager.current(context) == AppLanguage.ZH_CN) {
        paywallGuide
    } else {
        paywallGuideTraditional
    }
    MarkdownText(
        markdown = guide,
        color = OColor.black60,
    )
}

@Preview
@Composable
fun PreviewSubsRuleContent() {
    SubsRuleContent()
}
