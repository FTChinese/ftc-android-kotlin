package com.ft.ftchinese.ui.settings.overview

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingScreenTest {
    @Test
    fun languageRouteResolvesToLanguageScreen() {
        assertEquals(
            SettingScreen.Language,
            SettingScreen.fromRoute(SettingScreen.Language.name),
        )
    }
}
