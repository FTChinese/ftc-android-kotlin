package com.ft.ftchinese.splash

import com.beust.klaxon.Json

data class ScheduleMeta(

        val title: String,
        val description: String,
        val theme: String,
        val adid: String,
        val sponsorMobile: String,
        @Json(name = "fileTime")
        val lastModified: Long,
        val hideAd: String,
        val audiencePixelTag: String,
        val guideline: String
)