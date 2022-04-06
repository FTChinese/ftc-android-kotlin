package com.ft.ftchinese.model

import com.ft.ftchinese.BuildConfig

data class AppRelease(
    val versionName: String = "",
    val versionCode: Int,
    val body: String = "",
    val apkUrl: String = "",
) {
    val isNew: Boolean
        get() = versionCode > BuildConfig.VERSION_CODE

    fun splitBody(): List<String> {
        return body.split("\n")
            .map {
                it.removePrefix("*").trim()
            }
    }

    fun cacheFileName() = "release_log_$versionCode.json"
}
