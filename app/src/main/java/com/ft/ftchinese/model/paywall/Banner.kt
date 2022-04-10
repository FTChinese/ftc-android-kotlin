package com.ft.ftchinese.model.paywall

import com.ft.ftchinese.model.serializer.DateTimeAsStringSerializer
import kotlinx.serialization.Serializable
import org.threeten.bp.ZonedDateTime

@Serializable
data class Banner(
    val id: String,
    val heading: String,
    val subHeading: String? = null,
    val coverUrl: String? = null,
    val content: String? = null,
    val terms: String? = null,
    @Serializable(with = DateTimeAsStringSerializer::class)
    val startUtc: ZonedDateTime?,
    @Serializable(with = DateTimeAsStringSerializer::class)
    val endUtc: ZonedDateTime?
)
