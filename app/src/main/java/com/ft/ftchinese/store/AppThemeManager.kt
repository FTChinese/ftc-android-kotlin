package com.ft.ftchinese.store

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.ft.ftchinese.model.settings.AppTheme

object AppThemeManager {
    fun current(context: Context): AppTheme =
        SettingStore.getInstance(context).loadAppTheme() ?: AppTheme.SYSTEM

    fun apply(context: Context): AppTheme {
        val theme = current(context)
        val mode = when (theme) {
            AppTheme.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            AppTheme.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            AppTheme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        }
        if (AppCompatDelegate.getDefaultNightMode() != mode) {
            AppCompatDelegate.setDefaultNightMode(mode)
        }
        return theme
    }
}
