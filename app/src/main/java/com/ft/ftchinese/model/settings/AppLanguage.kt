package com.ft.ftchinese.model.settings

import android.content.res.Resources
import androidx.annotation.StringRes
import com.ft.ftchinese.R
import java.util.Locale

enum class AppLanguage(
    val serverTag: String,
    val resourceTag: String,
    @StringRes val labelId: Int,
) {
    ZH_CN("zh-CN", "zh-CN", R.string.app_language_simplified),
    ZH_TW("zh-TW", "zh-TW", R.string.app_language_traditional_taiwan),
    ZH_HK("zh-HK", "zh-Hant-HK", R.string.app_language_traditional_hong_kong);

    companion object {
        fun fromServerTag(tag: String): AppLanguage? {
            return values().firstOrNull { it.serverTag.equals(tag, ignoreCase = true) }
        }

        fun fromSystem(): AppLanguage {
            val locale = runCatching {
                Resources.getSystem().configuration.locales[0]
            }.getOrElse { Locale.getDefault() }
            return fromLocale(locale)
        }

        fun fromLocale(locale: Locale): AppLanguage {
            val tag = locale.toLanguageTag().lowercase(Locale.ROOT)
            val region = locale.country.uppercase(Locale.ROOT)
            val script = locale.script.lowercase(Locale.ROOT)

            if (region == "HK" || region == "MO" || tag.contains("hant-hk")) {
                return ZH_HK
            }
            if (region == "TW" || tag.contains("hant-tw") || script == "hant") {
                return ZH_TW
            }
            return ZH_CN
        }
    }
}
