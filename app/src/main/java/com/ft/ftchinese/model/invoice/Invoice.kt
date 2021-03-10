package com.ft.ftchinese.model.invoice

import com.ft.ftchinese.model.enums.*
import com.ft.ftchinese.model.fetch.*
import org.threeten.bp.ZonedDateTime

data class Invoice(
    val id: String,
    val compoundId: String,
    val tier: Tier,
    val cycle: Cycle,
    val years: Int,
    val months: Int,
    val days: Int,
    @KAddOnSource
    val addOnSource: AddOnSource? = null,
    val appleTxId: String? = null,
    var orderId: String? = null,
    @KOrderKind
    val orderKind: OrderKind? = null,
    val paidAmount: Double,
    @KPayMethod
    val payMethod: PayMethod? = null,
    val priceId: String? = null,
    val stripeSubsId: String? = null,
    @KDateTime
    val createdUtc: ZonedDateTime? = null,
    @KDateTime
    val consumedUtc: ZonedDateTime? = null,
    val startUtc: ZonedDateTime? = null,
    val endUtc: ZonedDateTime? = null,
    @KDateTime
    val carriedOverUtc: ZonedDateTime? = null,
) {
    fun withOrderId(id: String): Invoice {
        orderId = id
        return this
    }
}
