package com.ft.ftchinese.model.reader

import android.os.Parcelable
import com.beust.klaxon.Json
import kotlinx.parcelize.Parcelize

@Parcelize
data class ReadingDuration(
        val url: String,
        val refer: String,
        @Json(name = "timeIn")
        val startUnix: Long,
        @Json(name = "timeOut")
        val endUnix: Long,
        val userId: String,
        val functionName: String
) : Parcelable
