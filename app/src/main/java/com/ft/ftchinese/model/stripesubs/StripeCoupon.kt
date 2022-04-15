package com.ft.ftchinese.model.stripesubs

import com.ft.ftchinese.model.serializer.DateTimeAsStringSerializer
import kotlinx.serialization.Serializable
import org.threeten.bp.ZonedDateTime

@Serializable
data class StripeCoupon(
    val id: String,
    val amountOff: Int,
    val currency: String,
    val redeemBy: Long,
    val priceId: String?,
    @Serializable(with = DateTimeAsStringSerializer::class)
    val startUtc: ZonedDateTime? = null,
    @Serializable(with = DateTimeAsStringSerializer::class)
    val endUtc: ZonedDateTime? = null,
)
