package com.ft.ftchinese.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ft.ftchinese.ui.theme.OColor

@Composable
fun ProgressLayout(
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    content: @Composable () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize().then(modifier)
    ) {
        content()

        if (loading) {
            LinearProgressIndicator(
                color = OColor.claret,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
            )
        }
    }
}
