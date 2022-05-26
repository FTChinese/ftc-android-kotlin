package com.ft.ftchinese.ui.components

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import com.ft.ftchinese.ui.theme.OColor

@Composable
fun TipText(
    text: String,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.body2,
        color = OColor.black60
    )
}
