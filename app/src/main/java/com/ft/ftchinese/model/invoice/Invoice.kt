package com.ft.ftchinese.model.invoice

import com.ft.ftchinese.model.enums.*
import com.ft.ftchinese.model.ftcsubs.YearMonthDay
import com.ft.ftchinese.model.serializer.DateTimeAsStringSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString
import org.threeten.bp.ZonedDateTime

@Serializable
data class Invoice(
    val id: String,
    val compoundId: String,
    val tier: Tier,
    @Deprecated("Use YearMonthDay")
    val cycle: Cycle,
    val years: Int = 0,
    val months: Int = 0,
    val days: Int = 0,
    val addOnSource: AddOnSource? = null,
    val appleTxId: String? = null,
    @Transient
    val currency: String = "cny",
    var orderId: String? = null,
    val orderKind: OrderKind? = null,
    val paidAmount: Double,
    val payMethod: PayMethod? = null,
    val priceId: String? = null,
    val stripeSubsId: String? = null,
    @Serializable(with = DateTimeAsStringSerializer::class)
    val createdUtc: ZonedDateTime? = null,
    @Serializable(with = DateTimeAsStringSerializer::class)
    val consumedUtc: ZonedDateTime? = null,
    @Serializable(with = DateTimeAsStringSerializer::class)
    val startUtc: ZonedDateTime? = null,
    @Serializable(with = DateTimeAsStringSerializer::class)
    val endUtc: ZonedDateTime? = null,
    @Serializable(with = DateTimeAsStringSerializer::class)
    val carriedOverUtc: ZonedDateTime? = null,
) {

    fun toJsonString(): String {
        return kotlinx.serialization.json.Json.encodeToString(this)
    }

    val period: YearMonthDay
        get() = YearMonthDay(
            years = years,
            months = months,
            days = days,
        )

    fun withOrderId(id: String): Invoice {
        orderId = id
        return this
    }

    val totalDays: Int
        get() = years * 366 + months * 31 + days

    fun toAddOn(): AddOn {
        return when (tier) {
            Tier.STANDARD -> AddOn(
                standardAddOn = totalDays,
                premiumAddOn = 0,
            )
            Tier.PREMIUM -> AddOn(
                standardAddOn = 0,
                premiumAddOn = totalDays,
            )
        }
    }
}
