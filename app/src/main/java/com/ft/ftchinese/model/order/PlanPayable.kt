package com.ft.ftchinese.model.order

import android.os.Parcelable
import com.ft.ftchinese.util.GAAction
import com.ft.ftchinese.util.KCycle
import com.ft.ftchinese.util.KTier
import kotlinx.android.parcel.Parcelize

/**
 * PlanPayable can be used to build UI for checkout.
 */
@Parcelize
data class PlanPayable(
        @KTier
        val tier: Tier,
        @KCycle
        val cycle: Cycle,
        val listPrice: Double,
        val netPrice: Double,

        val balance: Double,
        val currency: String = "cny",
        val cycleCount: Long,
        val extraDays: Long,
        val payable: Double,

        var isUpgrade: Boolean = false,
        var isRenew: Boolean = false
): Parcelable {

    fun isPayRequired(): Boolean {
        return payable != 0.0
    }

    fun getId(): String {
        return "${tier.string()}_${cycle.string()}"
    }

    fun currencySymbol(): String {
        return when (currency) {
            "cny" -> "¥"
            "usd" -> "$"
            "gbp" -> "£"
            else -> "¥"
        }
    }

    fun gaAddCartAction(): String {
        return when (tier) {
            Tier.STANDARD -> when (cycle) {
                Cycle.YEAR -> GAAction.BUY_STANDARD_YEAR
                Cycle.MONTH -> GAAction.BUY_STANDARD_MONTH
            }
            Tier.PREMIUM -> GAAction.BUY_PREMIUM
        }
    }

    fun withStripePlan(p: StripePlan?): PlanPayable? {
        if (p == null) {
            return null
        }

        val price = (p.amount / 100).toDouble()
        return PlanPayable(
                tier = tier,
                cycle = Cycle.fromString(p.interval) ?: cycle,
                listPrice = price,
                netPrice = price,
                balance = balance,
                currency = p.currency,
                cycleCount = cycleCount,
                extraDays = extraDays,
                payable = price,
                isUpgrade = isUpgrade,
                isRenew = isRenew
        )
    }

    companion object {
        @JvmStatic
        fun fromPlan(p: Plan): PlanPayable {
            return PlanPayable(
                    tier = p.tier,
                    cycle = p.cycle,
                    listPrice = p.listPrice,
                    netPrice = p.netPrice,
                    balance = 0.0,
                    cycleCount = 1,
                    extraDays = 0,
                    payable = p.netPrice,
                    isUpgrade = false,
                    isRenew = false
            )
        }

        fun fromOrder(order: Order): PlanPayable {
            return PlanPayable(
                    tier = order.tier,
                    cycle = order.cycle,
                    listPrice = order.listPrice,
                    netPrice = order.netPrice,
                    balance = order.balance ?: 0.0,
                    cycleCount = order.cycleCount,
                    extraDays = order.extraDays,
                    payable = order.netPrice,
                    isUpgrade = order.usageType == OrderUsage.UPGRADE,
                    isRenew = order.usageType == OrderUsage.RENEW
            )
        }
    }
}
