package com.ft.ftchinese.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.ui.util.UriUtils

@Composable
fun rememberBaseUrl(
    account: Account?
) = remember(account) {
    UriUtils.discoverHost(account?.membership?.tier)
}
