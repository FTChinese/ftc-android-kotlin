package com.ft.ftchinese.model.subscription

import com.ft.ftchinese.util.KDateTime
import org.threeten.bp.ZonedDateTime

data class Discount(
    val id: String? = null,
    val priceOff: Double? = null,
    val percent: Int? = null,
    @KDateTime
    val startUtc: ZonedDateTime? = null,
    @KDateTime
    val endUtc: ZonedDateTime? = null
)
