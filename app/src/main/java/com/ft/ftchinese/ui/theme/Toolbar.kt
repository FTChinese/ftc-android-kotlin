package com.ft.ftchinese.ui.theme

import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun Toolbar(
    barTitle: String,
) {
    TopAppBar(
        elevation = Space.dp4,
    ) {
        Text(text = barTitle)
    }
}

@Preview
@Composable
fun PreviewToolbar() {
    Toolbar(barTitle = "Test")
}


