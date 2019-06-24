package com.ft.ftchinese.model

import com.beust.klaxon.Json

data class ReadingDuration(
        val url: String,
        val refer: String,
        @Json(name = "timeIn")
        val startUnix: Long,
        @Json(name = "timeOut")
        val endUnix: Long,
        val userId: String,
        val functionName: String
)
