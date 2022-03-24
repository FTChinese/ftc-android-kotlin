package com.ft.ftchinese.ui.paywall

import androidx.compose.runtime.Composable
import com.ft.ftchinese.model.paywall.paywallGuide
import dev.jeziellago.compose.markdowntext.MarkdownText


@Composable
fun SubsRuleContent() {
    MarkdownText(markdown = paywallGuide)
}
