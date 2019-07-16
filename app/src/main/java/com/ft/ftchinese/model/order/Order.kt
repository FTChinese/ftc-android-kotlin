package com.ft.ftchinese.model.order

import com.ft.ftchinese.util.*
import org.threeten.bp.LocalDate
import org.threeten.bp.ZonedDateTime


/**
 * An order created from server when user pressed pay button.
 */
data class Order(
        val id: String,
        @KTier
        val tier: Tier,
        @KCycle
        val cycle: Cycle,
        val netPrice: Double,
        @KPayMethod
        val payMethod: PayMethod,
        @KDateTime
        val createdAt: ZonedDateTime,
        @KDate
        val startDate: LocalDate,
        @KDate
        val endDate: LocalDate
)
