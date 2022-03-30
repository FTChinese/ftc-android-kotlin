package com.ft.ftchinese.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment

@Composable
fun ProgressLayout(
    loading: Boolean = false,
    content: @Composable () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center
    ) {
        content()

        if (loading) {
            LinearProgressIndicator()
        }
    }
}
