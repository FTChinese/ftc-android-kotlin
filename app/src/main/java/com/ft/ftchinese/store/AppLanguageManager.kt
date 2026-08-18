package com.ft.ftchinese.store

import android.content.Context
import android.util.Log
import com.ft.ftchinese.BuildConfig
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.ft.ftchinese.model.settings.AppLanguage

object AppLanguageManager {
    private const val TAG = "debug_app_language"

    fun current(context: Context): AppLanguage {
        val store = SettingStore.getInstance(context)
        val stored = store.loadAppLanguage()
        val system = AppLanguage.fromSystem()
        val effective = stored ?: system
        if (BuildConfig.DEBUG) {
            Log.i(
                TAG,
                "language_current stored=${stored?.serverTag ?: "<none>"} " +
                    "system=${system.serverTag} effective=${effective.serverTag} " +
                    "appLocales=${AppCompatDelegate.getApplicationLocales().toLanguageTags()}"
            )
        }
        return effective
    }

    fun apply(context: Context): AppLanguage {
        val language = current(context)
        val currentLocales = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (!currentLocales.equals(language.resourceTag, ignoreCase = true)) {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(language.resourceTag)
            )
        }
        if (BuildConfig.DEBUG) {
            Log.i(
                TAG,
                "language_apply effective=${language.serverTag} resource=${language.resourceTag} " +
                    "previousAppLocales=$currentLocales " +
                    "newAppLocales=${AppCompatDelegate.getApplicationLocales().toLanguageTags()}"
            )
        }
        return language
    }
}
