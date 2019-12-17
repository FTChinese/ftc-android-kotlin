package com.ft.ftchinese.model.order

import com.beust.klaxon.Json
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.subscription.Cycle
import com.ft.ftchinese.model.subscription.Tier
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

        val currency: String = "cny",

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

    fun withConfirmation(member: Membership): Subscription {
        val now = ZonedDateTime.now()
                .truncatedTo(ChronoUnit.SECONDS)
        val today = now.toLocalDate()

        val start = when {
            member.expireDate == null -> today
            usageType == OrderUsage.UPGRADE -> today
            member.expired() -> today
            else -> member.expireDate
        }

        confirmedAt = now
        startDate = start
        endDate = when (cycle) {
            Cycle.YEAR -> start.plusYears(cycleCount).plusDays(extraDays)
            Cycle.MONTH -> start.plusMonths(cycleCount).plusDays(extraDays)
        }

        return this
    }

    fun isConfirmed(): Boolean {
        return confirmedAt != null
    }
}

