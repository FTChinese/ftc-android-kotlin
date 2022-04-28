package com.ft.ftchinese.model

import com.ft.ftchinese.BuildConfig
import kotlinx.serialization.Serializable

@Serializable
data class AppRelease(
    val versionName: String = "",
    val versionCode: Int,
    val body: String? = null,
    val apkUrl: String = "",
) {
    val isNew: Boolean
        get() = versionCode > BuildConfig.VERSION_CODE

    val isValid: Boolean
        get() = apkUrl.startsWith("http")
}
