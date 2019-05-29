package com.ft.ftchinese.models

import com.beust.klaxon.Json
import com.ft.ftchinese.util.*
import org.threeten.bp.LocalDate
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.temporal.ChronoUnit

/**
 * This is used to store subscription order locally,
 * and also used to to parse orders retrieved from API.
 */
data class Subscription(
        val orderId: String,

        @KTier
        val tier: Tier,

        @KCycle
        val cycle: Cycle,

        // After supporting upgrading, the purchased membership
        // duration might not be exactly one cycle.
        val cycleCount: Long = 1,
        // 1 day less than server side so that we could compare
        // locally saved date against server data.
        val extraDays: Long = 0,

        var netPrice: Double,

        @KPayMethod
        val payMethod: PayMethod,

        @KDateTime
        val createdAt: ZonedDateTime = ZonedDateTime.now(),

        val isUpgrade: Boolean = false,

        @Json(ignored = true)
        var confirmedAt: ZonedDateTime? = null,

        @KDate
        var startDate: LocalDate? = null,

        @KDate
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
            isUpgrade -> today
            m.expireDate.isBefore(today) -> today
            else -> m.expireDate
        }

        confirmedAt = now

        setDuration(start)

        return Membership(tier = tier, cycle = cycle, expireDate = endDate)
    }
}

