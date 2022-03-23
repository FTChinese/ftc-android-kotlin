package com.ft.ftchinese.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable

private val LightColors = lightColors(
    primary = OColor.black60,
    primaryVariant = OColor.black30,
    secondary = OColor.claret,
    onSecondary = OColor.white,
    background = OColor.paper,
)

private val DarkColors = darkColors(
    primary = OColor.white60,
    primaryVariant = OColor.white20,
    background = OColor.black90,
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
