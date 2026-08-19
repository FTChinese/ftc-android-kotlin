package com.ft.ftchinese.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColors(
    primary = OColor.wheat, // Affects toolbar background
    onPrimary = OColor.black, // Affects toolbar content.
    background = OColor.paper,
)

private val DarkColors = darkColors(
    primary = OColor.black90,
    onPrimary = OColor.white,
    background = OColor.black,
    surface = OColor.black90,
    onBackground = OColor.white,
    onSurface = OColor.white,
)

@Composable
fun OTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    MaterialTheme(
        colors = if (darkTheme) DarkColors else LightColors,
        content = {
            val isLight = MaterialTheme.colors.isLight
            val backgroundColor = MaterialTheme.colors.background
            SideEffect {
                val window = view.context.findActivity()?.window ?: return@SideEffect
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.isAppearanceLightStatusBars = isLight
                controller.isAppearanceLightNavigationBars = isLight
                val color = backgroundColor.toArgb()
                window.statusBarColor = color
                window.navigationBarColor = color
            }
            content()
        }
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
