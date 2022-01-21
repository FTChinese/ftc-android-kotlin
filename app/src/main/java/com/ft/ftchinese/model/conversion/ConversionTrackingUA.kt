package com.ft.ftchinese.model.conversion

import android.os.Build
import com.ft.ftchinese.BuildConfig
import java.util.*

data class ConversionTrackingUA(
    val name: String,
    val version: String,
    val osAndVersion: String,
    val locale: String,
    val device: String,
    val build: String,
) {
    override fun toString(): String {
        return "$name/$version ($osAndVersion; $locale; $device; $build)"
    }
}
