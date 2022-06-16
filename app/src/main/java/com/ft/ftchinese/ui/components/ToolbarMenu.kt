package com.ft.ftchinese.ui.components

import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable

@Composable
fun MenuOpenInBrowser(
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick
    ) {
        IconOpenInBrowser()
    }
}
