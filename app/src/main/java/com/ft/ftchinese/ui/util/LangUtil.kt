package com.ft.ftchinese.ui.util

import android.content.res.Resources
import androidx.core.os.ConfigurationCompat
import java.util.*

object LangUtil {
    val TRADITIONAL_CHINESE = Locale.Builder().setLanguage("zh").setRegion("TW").setScript("hant").build()

    val locale = ConfigurationCompat.getLocales(Resources.getSystem().configuration).get(0)

    val isTC: Boolean
        get() = locale?.script == TRADITIONAL_CHINESE.script
}
