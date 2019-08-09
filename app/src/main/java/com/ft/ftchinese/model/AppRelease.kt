package com.ft.ftchinese.model

import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.util.KDateTime
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter

data class AppRelease(
        val versionName: String = "",
        val versionCode: Int = 0,
        val body: String = "",
        val apkUrl: String = "",
        @KDateTime
        val createdAt: ZonedDateTime? = null
) {
        val isNew: Boolean
                get() = versionCode > BuildConfig.VERSION_CODE

        val isCurrent: Boolean
                get() = versionCode == BuildConfig.VERSION_CODE

        val creationTime: String
                get() = createdAt?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) ?: ""
}
