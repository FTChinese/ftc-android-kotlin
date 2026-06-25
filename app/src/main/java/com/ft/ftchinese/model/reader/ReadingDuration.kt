package com.ft.ftchinese.model.reader

import android.os.Parcelable
import com.ft.ftchinese.BuildConfig
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class ReadingDuration(
    val url: String,
    val refer: String,
    @SerialName("timeIn")
    val startUnix: Long,
    @SerialName("timeOut")
    val endUnix: Long,
    val userId: String,
    val functionName: String,
    val appVersion: String = BuildConfig.VERSION_NAME,
    val appVersionCode: Int = BuildConfig.VERSION_CODE
) : Parcelable
