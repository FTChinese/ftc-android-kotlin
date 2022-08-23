package com.ft.ftchinese.model.content

import androidx.annotation.StringRes
import com.ft.ftchinese.R

enum class Language(
    val symbol: String,
    @StringRes val nameId: Int
) {
    CHINESE("cn", R.string.titlebar_cn),
    ENGLISH("en", R.string.titlebar_en),
    BILINGUAL("bi", R.string.titlebar_bilingual);

    fun aiAudioPathSuffix(): String {
        return when (this) {
            BILINGUAL -> ENGLISH.symbol
            else -> symbol
        }
    }

    companion object {
        private val map = Language.values().associateBy(Language::symbol)
        fun fromSymbol(symbol: String) = map[symbol]
    }

}


