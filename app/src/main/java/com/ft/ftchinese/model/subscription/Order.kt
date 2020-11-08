package com.ft.ftchinese.model.subscription

import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.util.*
import org.threeten.bp.LocalDate
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.temporal.ChronoUnit

/**
 * This is used to store subscription order locally,
 * and also used to to parse orders retrieved from API.
 */
open class Order(
    open val id: String,

    val ftcId: String? = null,
    val unionId: String? = null,
    val planId: String? = null,
    val discountId: String? = null,
    val price: Double? = null,

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

    val totalBalance: Double = 0.0,

    @KDateTime
    open val createdAt: ZonedDateTime = ZonedDateTime.now(),

    @KDateTime
    var confirmedAt: ZonedDateTime? = null,

    @KDate
    var startDate: LocalDate? = null,

    @KDate
    var endDate: LocalDate? = null
) {

    fun isConfirmed(): Boolean {
        return confirmedAt != null
    }

    fun confirm(member: Membership): ConfirmationResult {
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


        return ConfirmationResult(
            order = this,
            membership = Membership(
                tier = tier,
                cycle = cycle,
                expireDate = endDate,
                payMethod = payMethod,
                autoRenew = false,
                status = null,
                vip = false
            )
        )
    }
}

data class AliOrder(
    val order: Order,
    val param: String

)

data class WxPayParams(
    val appId: String,
    val partnerId: String,
    val prepayId: String,
    val timestamp: String,
    val nonce: String,
    val pkg: String,
    val signature: String
)

// This is user's payment intent.
data class WxPayIntent(
    val order: Order,
    val params: WxPayParams
)
