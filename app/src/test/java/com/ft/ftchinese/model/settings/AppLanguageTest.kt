package com.ft.ftchinese.model.settings

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {
    @Test
    fun nonChineseSystemLanguagesFallBackToSimplifiedChinese() {
        assertEquals(AppLanguage.ZH_CN, AppLanguage.fromLocale(Locale.US))
        assertEquals(AppLanguage.ZH_CN, AppLanguage.fromLocale(Locale.GERMANY))
    }

    @Test
    fun traditionalRegionsUseTheirChineseVariant() {
        assertEquals(AppLanguage.ZH_TW, AppLanguage.fromLocale(Locale.TAIWAN))
        assertEquals(AppLanguage.ZH_HK, AppLanguage.fromLocale(Locale("zh", "HK")))
    }

    @Test
    fun simplifiedChineseIsTheDefaultChineseVariant() {
        assertEquals(AppLanguage.ZH_CN, AppLanguage.fromLocale(Locale.SIMPLIFIED_CHINESE))
    }
}
