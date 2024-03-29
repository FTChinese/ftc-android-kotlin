package com.ft.ftchinese.model.ftcsubs

import com.ft.ftchinese.model.enums.AddOnSource
import com.ft.ftchinese.model.enums.OrderKind
import com.ft.ftchinese.model.enums.PurchaseAction
import com.ft.ftchinese.model.invoice.Invoice
import com.ft.ftchinese.model.reader.Membership
import kotlinx.serialization.Serializable
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

// Purchase action: buy|renew|winback
@Serializable
data class ConfirmationResult (
    val order: Order,
    val membership: Membership, // Latest membership
    val snapshot: Membership, // Prior membership
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

        val ymd = order.period

        return Invoice(
            id = "",
            compoundId = "",
            tier = order.tier,
            cycle = ymd.toCycle(),
            years = ymd.years,
            months = ymd.months,
            days = ymd.days,
            addOnSource = if (order.kind == OrderKind.AddOn) {
                AddOnSource.UserPurchase
            } else null,
            appleTxId = null,
            orderId = order.id,
            orderKind = order.kind,
            paidAmount = order.payableAmount,
            payMethod = order.payMethod,
            stripeSubsId = null,
            createdUtc = ZonedDateTime.now(),
            consumedUtc = if (order.kind != OrderKind.AddOn) {
                ZonedDateTime.now()
            } else null,
            startUtc = startDateTime,
            endUtc = startDateTime?.plus(ymd.period()),
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
