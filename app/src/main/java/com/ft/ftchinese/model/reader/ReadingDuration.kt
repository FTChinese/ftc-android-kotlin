package com.ft.ftchinese.model.reader

import android.os.Parcelable
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
    val functionName: String
) : Parcelable
