package com.ft.ftchinese.model.ftcsubs

import com.ft.ftchinese.model.enums.OrderKind
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.fetch.*
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
    @KTier
    val tier: Tier,
    @KOrderKind
    val kind: OrderKind,
    val originalPrice: Double? = null,
    val payableAmount: Double,
    @KPayMethod
    val payMethod: PayMethod,
    val yearsCount: Int,
    val monthsCount: Int,
    val daysCount: Int,
    @KDateTime
    val createdAt: ZonedDateTime = ZonedDateTime.now(),
    @KDateTime
    var confirmedAt: ZonedDateTime? = null,
    @KDate
    var startDate: LocalDate? = null,
    @KDate
    var endDate: LocalDate? = null,
    val currency: String = "cny", // Not included when getting order list.
) {

    fun toJsonString(): String {
        return json.toJsonString(this)
    }

    val period: YearMonthDay
        get() = YearMonthDay(
            years = yearsCount,
            months = monthsCount,
            days = daysCount
        )

    // Checks if an order has confirmedAt field set.
    fun isConfirmed(): Boolean {
        return confirmedAt != null
    }

    // Confirm this order is paid.
    // Used to build new order in ConfirmationResult.
    fun confirmed(
        at: ZonedDateTime,
        start: LocalDate?,
        end: LocalDate?
    ): Order {
        confirmedAt = at
        startDate = start
        endDate = end
        return this
    }
}
