package com.ft.ftchinese.model.stripesubs

import com.ft.ftchinese.model.serializer.DateTimeAsStringSerializer
import kotlinx.serialization.Serializable
import org.threeten.bp.ZonedDateTime

@Serializable
data class CouponApplied(
    val invoiceId: String,
    val ftcId: String,
    val subsId: String,
    val couponId: String,
    @Serializable(with = DateTimeAsStringSerializer::class)
    val createdUtc: ZonedDateTime? = null,
    @Serializable(with = DateTimeAsStringSerializer::class)
    val redeemedUtc: ZonedDateTime? = null,
)
