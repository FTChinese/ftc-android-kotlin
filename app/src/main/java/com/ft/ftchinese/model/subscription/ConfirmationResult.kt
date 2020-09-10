package com.ft.ftchinese.model.subscription

import com.ft.ftchinese.model.reader.Membership
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.temporal.ChronoUnit

data class ConfirmationResult (
        val order: Order,
        val membership: Membership
)

/**
 * Confirms order created by ali or wechat.
 * This is a fallback if server encountered errors.
 * Local access is temporarily enabled as soon as
 * payment is done.
 */
fun confirmOrder(order: Order, member: Membership): ConfirmationResult {
        val now = ZonedDateTime.now()
                .truncatedTo(ChronoUnit.SECONDS)
        val today = now.toLocalDate()

        val start = when {
                member.expireDate == null -> today
                order.usageType == OrderUsage.UPGRADE -> today
                member.expired() -> today
                else -> member.expireDate
        }

        order.confirmedAt = now
        order.startDate = start
        order.endDate = when (order.cycle) {
                Cycle.YEAR -> start.plusYears(order.cycleCount).plusDays(order.extraDays)
                Cycle.MONTH -> start.plusMonths(order.cycleCount).plusDays(order.extraDays)
        }


        return ConfirmationResult(
                order = order,
                membership = Membership(
                        tier = order.tier,
                        cycle = order.cycle,
                        expireDate = order.endDate,
                        payMethod = order.payMethod,
                        autoRenew = false,
                        status = null,
                        vip = false
                )
        )
}
