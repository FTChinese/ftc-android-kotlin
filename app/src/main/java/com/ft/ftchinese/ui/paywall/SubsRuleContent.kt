package com.ft.ftchinese.ui.paywall

import androidx.compose.runtime.Composable
import dev.jeziellago.compose.markdowntext.MarkdownText


@Composable
fun SubsRuleContent() {
    MarkdownText(markdown = paywallGuide)
}
