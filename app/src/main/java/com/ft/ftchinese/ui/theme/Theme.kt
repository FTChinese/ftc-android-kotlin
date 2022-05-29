package com.ft.ftchinese.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColors(
    background = OColor.paper,
)

private val DarkColors = darkColors(
    background = OColor.black80,
)

@Composable
fun OTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = LightColors,
        content = content
    )
}

object OButton {
    @Composable
    fun primaryButtonColors(
        backgroundColor: Color = OColor.teal,
        contentColor: Color = OColor.white,
    ): ButtonColors = ButtonDefaults.buttonColors(
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        disabledBackgroundColor = backgroundColor.copy(alpha = 0.4F)
    )

    @Composable
    fun outlinedColors(
        backgroundColor: Color = Color.Transparent,
        contentColor: Color = OColor.teal,
    ): ButtonColors = ButtonDefaults.outlinedButtonColors(
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        disabledContentColor = contentColor.copy(alpha = 0.4F)
    )

    @Composable
    fun textColors(
        backgroundColor: Color = Color.Transparent,
        contentColor: Color = OColor.teal,
    ): ButtonColors = ButtonDefaults.textButtonColors(
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        disabledContentColor = contentColor.copy(alpha = 0.4F),
    )
}
