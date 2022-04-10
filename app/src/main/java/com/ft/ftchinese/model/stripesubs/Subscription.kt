package com.ft.ftchinese.model.stripesubs

import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.StripeSubStatus
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.serializer.DateTimeAsStringSerializer
import kotlinx.serialization.Serializable
import org.threeten.bp.ZonedDateTime

@Serializable
data class Subscription(
    val id: String,
    val tier: Tier,
    val cycle: Cycle,
    @Serializable(with = DateTimeAsStringSerializer::class)
    val cancelAtUtc: ZonedDateTime? = null,
    val cancelAtPeriodEnd: Boolean,
    @Serializable(with = DateTimeAsStringSerializer::class)
    val canceledUtc: ZonedDateTime? = null,
    @Serializable(with = DateTimeAsStringSerializer::class)
    val currentPeriodEnd: ZonedDateTime,
    @Serializable(with = DateTimeAsStringSerializer::class)
    val currentPeriodStart: ZonedDateTime,
    val customerId: String,
    val defaultPaymentMethod: String? = null,
//    val subsItemId: String,
//    val priceId: String,
    val latestInvoiceId: String,
    val liveMode: Boolean,
    @Serializable(with = DateTimeAsStringSerializer::class)
    val startDateUtc: ZonedDateTime? = null,
    @Serializable(with = DateTimeAsStringSerializer::class)
    val endedUtc: ZonedDateTime? = null,
//    @KDateTime
//    val createdUtc: ZonedDateTime? = null,
//    @KDateTime
//    val updatedUtc: ZonedDateTime? = null,
    val status: StripeSubStatus? = null,
    val ftcUserId: String? = null,
    val paymentIntent: PaymentIntent? = null,
)
