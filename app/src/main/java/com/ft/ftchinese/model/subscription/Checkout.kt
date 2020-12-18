package com.ft.ftchinese.model.subscription

import com.ft.ftchinese.util.KOrderUsage

data class CheckedItem(
    val plan: Plan,
    val discount: Discount,
)

data class Duration(
    val cycleCount: Int = 1,
    val extraDays: Int = 0,
)

data class Charge(
    val amount: Double,
    val currency: String = "cny"
)

data class Checkout(
    @KOrderUsage
    val kind: OrderKind,
    val item: CheckedItem,
    val wallet: Wallet,
    val duration: Duration,
    val payable: Charge,
    val isFree: Boolean,
    val live: Boolean,
) {
    fun upgradePlan(): Plan {
        return Plan(
            id = item.plan.id,
            productId = item.plan.productId,
            price = payable.amount,
            tier = item.plan.tier,
            cycle = item.plan.cycle,
            discount = Discount()
        )
    }
}
