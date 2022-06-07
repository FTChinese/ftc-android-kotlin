package com.ft.ftchinese.model

import com.ft.ftchinese.BuildConfig
import kotlinx.serialization.Serializable

@Serializable
data class AppRelease(
    val versionName: String = "",
    val versionCode: Int,
    val body: String? = null,
    val apkUrl: String = "",
) : Comparable<AppRelease> {
    val isNew: Boolean
        get() = versionCode > BuildConfig.VERSION_CODE

    val isValid: Boolean
        get() = apkUrl.startsWith("http")

    override fun compareTo(other: AppRelease): Int {
        return versionCode - other.versionCode
    }
}

data class AppDownloaded(
    val release: AppRelease,
    val downloadId: Long,
)
