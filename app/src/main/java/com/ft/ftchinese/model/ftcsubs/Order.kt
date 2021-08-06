package com.ft.ftchinese.model.ftcsubs

import com.beust.klaxon.Json
import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.OrderKind
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.fetch.*
import com.ft.ftchinese.model.price.Edition
import org.threeten.bp.LocalDate
import org.threeten.bp.ZonedDateTime

/**
 * This is used to store subscription order locally,
 * and also used to to parse orders retrieved from API.
 */
data class Order(
    val id: String,
    val ftcId: String? = null,
    val unionId: String? = null,
    val priceId: String? = null,
    val discountId: String? = null,
    val price: Double? = null,
    @KTier
    val tier: Tier,
    @KCycle
    val cycle: Cycle,
    var amount: Double, // Why this is var?
    val currency: String = "cny", // Not included when getting order list.
    @KOrderKind
    val kind: OrderKind,
    @KPayMethod
    val payMethod: PayMethod,
    @KDateTime
    val createdAt: ZonedDateTime = ZonedDateTime.now(),
    @KDateTime
    var confirmedAt: ZonedDateTime? = null,
    @KDate
    var startDate: LocalDate? = null,
    @KDate
    var endDate: LocalDate? = null
) {

    fun toJsonString(): String {
        return json.toJsonString(this)
    }

    @Json(ignored = true)
    val edition: Edition
        get() = Edition(
            tier = tier,
            cycle = cycle,
        )

    fun isConfirmed(): Boolean {
        return confirmedAt != null
    }

    fun confirmed(at: ZonedDateTime, start: LocalDate?, end: LocalDate?): Order {
        confirmedAt = at
        startDate = start
        endDate = end
        return this
    }
}
