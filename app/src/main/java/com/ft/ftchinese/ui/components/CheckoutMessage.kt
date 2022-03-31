package com.ft.ftchinese.ui.components

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import com.ft.ftchinese.ui.theme.OColor

@Composable
fun CheckoutMessage(
    text: String,
) {
    if (text.isNotBlank()) {
        Text(
            text = text,
            style = MaterialTheme.typography.body2,
            color = OColor.claret,
        )
    }
}
