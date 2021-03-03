package com.ft.ftchinese.model.ftcsubs

import com.ft.ftchinese.model.enums.*
import com.ft.ftchinese.model.fetch.*
import com.ft.ftchinese.model.price.Edition
import com.ft.ftchinese.model.reader.Membership
import org.threeten.bp.LocalDate
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.temporal.ChronoUnit

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
    @KOrderUsage
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

    val edition: Edition
        get() = Edition(
            tier = tier,
            cycle = cycle,
        )

    fun isConfirmed(): Boolean {
        return confirmedAt != null
    }

    fun confirm(member: Membership): ConfirmationResult {
        val now = ZonedDateTime.now()
            .truncatedTo(ChronoUnit.SECONDS)
        val today = now.toLocalDate()

        val start = when {
            member.expireDate == null -> today
            kind == OrderKind.Upgrade -> today
            member.autoRenewOffExpired -> today
            else -> member.expireDate
        }

        confirmedAt = now
        startDate = start
        endDate = when (cycle) {
            Cycle.YEAR -> start.plusYears(1).plusDays(1)
            Cycle.MONTH -> start.plusMonths(1).plusDays(1)
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

data class AliPayIntent(
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
