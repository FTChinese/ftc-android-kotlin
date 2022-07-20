package com.ft.ftchinese.ui.theme

import androidx.compose.material.ContentAlpha
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

    val black80or30: Color
        @Composable
        get() = if (MaterialTheme.colors.isLight) {
            OColor.black80
        } else {
            OColor.black30
        }

    val black50Default: Color
        @Composable
        get() = if (MaterialTheme.colors.isLight) {
            OColor.black50
        } else {
            Color.Unspecified
        }

    val whiteBlack90: Color
        @Composable
        get() = if (MaterialTheme.colors.isLight) {
            OColor.white
        } else {
            OColor.black90
        }

    val popupOverlay: Color
        @Composable
        get() = if (MaterialTheme.colors.isLight) {
            OColor.black.copy(ContentAlpha.disabled)
        } else {
            OColor.white.copy(ContentAlpha.disabled)
        }
}
