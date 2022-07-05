package com.ft.ftchinese.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object OColors {
    val claretWhite: Color
        @Composable
        get() = if (MaterialTheme.colors.isLight) {
            OColor.claret
        } else {
            OColor.white
        }

    val black60or30: Color
        @Composable
        get() = if (MaterialTheme.colors.isLight) {
            OColor.black60
        } else {
            OColor.black30
        }
}
