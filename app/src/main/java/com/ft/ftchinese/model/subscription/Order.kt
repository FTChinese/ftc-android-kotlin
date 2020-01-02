package com.ft.ftchinese.model.subscription

import com.beust.klaxon.Json
import com.ft.ftchinese.util.*
import org.threeten.bp.LocalDate
import org.threeten.bp.ZonedDateTime

/**
 * This is used to store subscription order locally,
 * and also used to to parse orders retrieved from API.
 */
open class Order(
        open val id: String,

        @KTier
        open val tier: Tier,

        @KCycle
        open val cycle: Cycle,

        // Charge
        open var amount: Double, // Why this is var?

        // Not included when getting order list.
        open val currency: String = "cny",

        // Duration
        // After supporting upgrading, the purchased membership
        // duration might not be exactly one cycle.
        // Not included when getting order list
        open val cycleCount: Long = 1,
        // 1 day less than server side so that we could compare
        // locally saved date against server data.
        // Not included when getting order list.
        open val extraDays: Long = 0,

        @KOrderUsage
        open val usageType: OrderUsage,

        @KPayMethod
        open val payMethod: PayMethod,

        @KDateTime
        open val createdAt: ZonedDateTime = ZonedDateTime.now(),

        // Not included when getting order list.
        @Json(ignored = true)
        var confirmedAt: ZonedDateTime? = null,

        // The following two fields are only returned
        // from getting order list API.
        @KDate
        var startDate: LocalDate? = null,

        @KDate
        var endDate: LocalDate? = null
) {

    fun isConfirmed(): Boolean {
        return confirmedAt != null
    }
}

