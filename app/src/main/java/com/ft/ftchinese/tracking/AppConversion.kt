package com.ft.ftchinese.tracking

import android.os.Build
import java.util.*

object AppConversion {
    private val name = "AdWords"
    private val version = "7.10.1" // TODO: what is this?
    private val osAndVersion = "Android ${Build.VERSION.RELEASE}"
    private val locale = Locale.getDefault()
    private val device = Build.MODEL
    private val build = "Build/${Build.ID}"

    val userAgent: String
        get() = "$name $version ($osAndVersion; $locale; $device; $build)"
}
