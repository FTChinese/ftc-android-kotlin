package com.ft.ftchinese.model.ftcsubs

import com.beust.klaxon.Json
import com.ft.ftchinese.model.enums.AddOnSource
import com.ft.ftchinese.model.enums.OrderKind
import com.ft.ftchinese.model.enums.PurchaseAction
import com.ft.ftchinese.model.invoice.Invoice
import com.ft.ftchinese.model.reader.Membership
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

// Purchase action: buy|renew|winback
data class ConfirmationResult (
    val order: Order,
    val membership: Membership, // Latest membership
    val snapshot: Membership, // Prior membership
    @Json(ignored = true)
    val invoices: Invoices,
    val action: PurchaseAction
)

data class ConfirmationParams(
    val order: Order,
    val member: Membership, // Membership prior purchase.
    val confirmedAt: ZonedDateTime = ZonedDateTime.now(),
) {
    private fun purchaseInvoice(): Invoice {
        val startDateTime = when (order.kind) {
            OrderKind.Create, OrderKind.Renew -> if (member.expireDate != null) {
                val expDateTime = member.expireDate.atStartOfDay(ZoneId.systemDefault())
                if (expDateTime.isBefore(confirmedAt)) {
                    confirmedAt
                } else {
                    expDateTime
                }
            } else {
                confirmedAt
            }
            OrderKind.Upgrade -> confirmedAt
            OrderKind.AddOn -> null
            else -> null
        }

        val period = order.cycle.period

        return Invoice(
            id = "",
            compoundId = "",
            tier = order.tier,
            cycle = order.cycle,
            years = period.years,
            months = period.months,
            days = period.days,
            addOnSource = if (order.kind == OrderKind.AddOn) {
                AddOnSource.UserPurchase
            } else null,
            appleTxId = null,
            orderId = order.id,
            orderKind = order.kind,
            paidAmount = order.amount,
            payMethod = order.payMethod,
            priceId = order.priceId,
            stripeSubsId = null,
            createdUtc = ZonedDateTime.now(),
            consumedUtc = if (order.kind != OrderKind.AddOn) {
                ZonedDateTime.now()
            } else null,
            startUtc = startDateTime,
            endUtc = startDateTime?.plus(period),
            carriedOverUtc = null,
        )
    }

    private fun carryOverInvoice(): Invoice? {
        return if (order.kind == OrderKind.Upgrade) {
            return member.carryOverInvoice()?.withOrderId(order.id)
        } else {
            null
        }
    }

    val invoices: Invoices
        get() = Invoices(
            purchased = purchaseInvoice(),
            carriedOver = carryOverInvoice()
        )

    // Used to build confirmation url to collect user info.
    private val purchaseAction = when (order.kind) {
        OrderKind.Renew, OrderKind.Upgrade, OrderKind.AddOn -> PurchaseAction.RENEW
        OrderKind.Create -> if (member.expireDate == null) {
            PurchaseAction.BUY
        } else {
            PurchaseAction.WIN_BACK
        }
        else -> PurchaseAction.RENEW
    }

    fun buildResult(): ConfirmationResult {

        val inv = invoices

        return ConfirmationResult(
            order = order.confirmed(
                at = confirmedAt,
                start = inv.purchased.startUtc?.toLocalDate(),
                end = inv.purchased.endUtc?.toLocalDate(),
            ),
            membership = inv.membership(member),
            snapshot = member,
            invoices = inv,
            action = purchaseAction,
        )
    }
}
