package com.ft.ftchinese.ui.components

import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import com.ft.ftchinese.R

@Composable
fun MenuOpenInBrowser(
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_baseline_open_in_browser_24),
            contentDescription = "Open in browser"
        )
    }
}
