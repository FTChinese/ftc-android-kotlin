package com.ft.ftchinese.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.repository.Config

@Composable
fun rememberBaseUrl(
    account: Account?
) = remember(account) {
    Config.discoverServer(account)
}
