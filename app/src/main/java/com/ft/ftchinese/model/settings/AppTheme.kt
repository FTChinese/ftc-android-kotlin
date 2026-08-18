package com.ft.ftchinese.model.settings

import androidx.annotation.StringRes
import com.ft.ftchinese.R

enum class AppTheme(
    val key: String,
    @StringRes val labelId: Int,
) {
    SYSTEM("system", R.string.app_theme_system),
    DARK("dark", R.string.app_theme_dark),
    LIGHT("light", R.string.app_theme_light);

    companion object {
        fun fromKey(key: String): AppTheme? = values().firstOrNull { it.key == key }
    }
}
