package com.ft.ftchinese.store

import android.content.Context
import androidx.core.content.edit
import com.ft.ftchinese.model.content.Language
import com.ft.ftchinese.model.enums.FontSize
import com.ft.ftchinese.model.settings.AppLanguage
import com.ft.ftchinese.model.settings.AppTheme

private const val SETTING_PREF_NAME = "com.ft.ftchinese.settings"
private const val PREF_FONT_SIZE = "font_size"
private const val PREF_SELECTED_LANG = "selected_language"
private const val PREF_APP_LANGUAGE = "app_language"
private const val PREF_APP_THEME = "app_theme"

class SettingStore private constructor(context: Context) {
    private val sharedPref = context.getSharedPreferences(SETTING_PREF_NAME, Context.MODE_PRIVATE)

    /**
     * Possible values:
     * - smallest
     * - smaller
     * - normal, default.
     * - bigger
     * - biggest
     */
    fun saveFontSize(size: FontSize) {
        sharedPref.edit(commit = true) {
            putString(PREF_FONT_SIZE, size.key)
        }
    }

    fun loadFontSize(): FontSize {
        val key = sharedPref.getString(PREF_FONT_SIZE, null) ?: return FontSize.Normal

        return FontSize.fromKey(key) ?: FontSize.Normal
    }

    fun saveLang(l: Language) {
        sharedPref.edit(commit = true) {
            putString(PREF_SELECTED_LANG, l.symbol)
        }
    }

    fun loadLang(): Language {
        val langStr =  sharedPref.getString(PREF_SELECTED_LANG, null) ?: return Language.CHINESE

        return Language.fromSymbol(langStr) ?: Language.CHINESE
    }

    fun saveAppLanguage(language: AppLanguage) {
        sharedPref.edit(commit = true) {
            putString(PREF_APP_LANGUAGE, language.serverTag)
        }
    }

    fun loadAppLanguage(): AppLanguage? {
        val tag = sharedPref.getString(PREF_APP_LANGUAGE, null) ?: return null
        return AppLanguage.fromServerTag(tag)
    }

    fun saveAppTheme(theme: AppTheme) {
        sharedPref.edit(commit = true) {
            putString(PREF_APP_THEME, theme.key)
        }
    }

    fun loadAppTheme(): AppTheme? {
        val key = sharedPref.getString(PREF_APP_THEME, null) ?: return null
        return AppTheme.fromKey(key)
    }

    companion object {
        private var instance: SettingStore? = null

        @Synchronized fun getInstance(ctx: Context): SettingStore {
            if (instance == null) {
                instance = SettingStore(ctx.applicationContext)
            }

            return instance!!
        }
    }
}
