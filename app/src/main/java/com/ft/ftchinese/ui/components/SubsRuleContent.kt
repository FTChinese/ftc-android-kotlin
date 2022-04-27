package com.ft.ftchinese.ui.components

import androidx.compose.runtime.Composable
import com.ft.ftchinese.model.paywall.paywallGuide
import com.ft.ftchinese.ui.theme.OColor
import dev.jeziellago.compose.markdowntext.MarkdownText

@Composable
fun SubsRuleContent() {
    MarkdownText(
        markdown = paywallGuide,
        color = OColor.black60,
    )
}
