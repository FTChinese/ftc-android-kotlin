package com.ft.ftchinese.model.paywall

import com.ft.ftchinese.model.fetch.KDateTime
import org.threeten.bp.ZonedDateTime

data class Banner(
    val id: String,
    val heading: String,
    val subHeading: String? = null,
    val coverUrl: String? = null,
    val content: String? = null,
    val terms: String? = null,
    @KDateTime
    val startUtc: ZonedDateTime?,
    @KDateTime
    val endUtc: ZonedDateTime?
)
