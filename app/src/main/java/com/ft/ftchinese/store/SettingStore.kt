package com.ft.ftchinese.store

import android.content.Context
import androidx.core.content.edit
import com.ft.ftchinese.model.content.Language

private const val SETTING_PREF_NAME = "com.ft.ftchinese.settings"
private const val PREF_FONT_SIZE = "font_size"
private const val PREF_SELECTED_LANG = "selected_language"

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
    fun saveFontSize(size: String) {
        sharedPref.edit(commit = true) {
            putString(PREF_FONT_SIZE, size)
        }
    }

    fun loadFontSize(): String {
        return sharedPref.getString(PREF_FONT_SIZE, null) ?: "normal"
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
