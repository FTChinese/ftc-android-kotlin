package com.ft.ftchinese.model.subscription

import com.ft.ftchinese.model.enums.OrderKind
import com.ft.ftchinese.model.fetch.KOrderUsage

data class CheckoutItem(
    val plan: Plan,
    val discount: Discount? = null, // A discount attached to the Plan but only valid for current moment.
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
    val item: CheckoutItem,
    val wallet: Wallet,
    val duration: Duration,
    val payable: Charge,
    val isFree: Boolean,
    val live: Boolean,
)
