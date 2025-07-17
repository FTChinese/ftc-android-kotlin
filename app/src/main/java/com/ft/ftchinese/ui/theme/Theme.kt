package com.ft.ftchinese.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable

private val LightColors = lightColors(
    primary = OColor.wheat, // Affects toolbar background
    onPrimary = OColor.black, // Affects toolbar content.
    background = OColor.paper,
)

private val DarkColors = darkColors(
    primary = OColor.wheat,       // Use same wheat color to stay consistent
    onPrimary = OColor.black,
    background = OColor.paper,    // Optional: use a different dark color if preferred
)

@Composable
fun OTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}

