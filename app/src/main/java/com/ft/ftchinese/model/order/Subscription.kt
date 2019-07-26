package com.ft.ftchinese.model.order

import com.beust.klaxon.Json
import com.ft.ftchinese.model.Membership
import com.ft.ftchinese.util.*
import org.threeten.bp.LocalDate
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.temporal.ChronoUnit

/**
 * This is used to store subscription order locally,
 * and also used to to parse orders retrieved from API.
 */
open class Subscription(
        open val id: String,

        @KTier
        open val tier: Tier,

        @KCycle
        open val cycle: Cycle,

        // After supporting upgrading, the purchased membership
        // duration might not be exactly one cycle.
        open val cycleCount: Long = 1,
        // 1 day less than server side so that we could compare
        // locally saved date against server data.
        open val extraDays: Long = 0,

        open var amount: Double,

        @KOrderUsage
        open val usageType: OrderUsage,

        @KPayMethod
        open val payMethod: PayMethod,

        @KDateTime
        open val createdAt: ZonedDateTime = ZonedDateTime.now(),

//        val isUpgrade: Boolean = false,

        @Json(ignored = true)
        var confirmedAt: ZonedDateTime? = null,

        @KDate
        @Json(ignored = true)
        var startDate: LocalDate? = null,

        @KDate
        @Json(ignored = true)
        var endDate: LocalDate? = null
) {

    private fun setDuration(start: LocalDate) {
        startDate = start

        endDate = when (cycle) {
            Cycle.YEAR -> start.plusYears(cycleCount).plusDays(extraDays)
            Cycle.MONTH -> start.plusMonths(cycleCount).plusDays(extraDays)
        }
    }

    /**
     * Confirm a subscription and return the new Membership.
     */
    fun confirm(m: Membership): Membership {
        val now = ZonedDateTime.now()
                .truncatedTo(ChronoUnit.SECONDS)
        val today = now.toLocalDate()

        // If current membership does not have expire date,
        // it means user is not member currently, use
        // today as startDate;
        // For upgrading the starting date is also today.
        val start = when {
            m.expireDate == null -> today
            usageType == OrderUsage.UPGRADE -> today
            m.expireDate.isBefore(today) -> today
            else -> m.expireDate
        }

        confirmedAt = now

        setDuration(start)

        return Membership(tier = tier, cycle = cycle, expireDate = endDate)
    }

    /**
     * Get an subscription's original plan
     */
    fun plan(): Plan {
        return Plan(
                tier = tier,
                cycle = cycle,
                cycleCount = cycleCount,
                currency = "cny",
                extraDays = extraDays,
                listPrice = amount,
                netPrice = amount,
                description = ""
        )
    }
}

