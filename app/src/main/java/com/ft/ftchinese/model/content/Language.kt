package com.ft.ftchinese.model.content

enum class Language(val symbol: String) {
    CHINESE("cn"),
    ENGLISH("en"),
    BILINGUAL("bi");

    fun aiAudioPathSuffix(): String {
        return when (this) {
            BILINGUAL -> ENGLISH.symbol
            else -> symbol
        }
    }
}
