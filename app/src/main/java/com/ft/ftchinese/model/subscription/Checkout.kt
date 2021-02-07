package com.ft.ftchinese.model.subscription

import com.ft.ftchinese.model.enums.OrderKind
import com.ft.ftchinese.model.fetch.KOrderUsage
import com.ft.ftchinese.model.ui.PriceParams

data class CheckoutItem(
    val plan: Plan,
    val discount: Discount? = null, // A discount attached to the Plan but only valid for current moment.
) {
    // Parameters to build the original price.
    val originalPriceParams: PriceParams
        get() = PriceParams(
            currency = plan.currency,
            amount = plan.price,
            cycle = plan.cycle,
            tier = plan.tier,
        )

    // Parameters to build payable price after deducting discount.
    val payablePriceParams: PriceParams
        get() = PriceParams(
            currency = plan.currency,
            amount = plan.price - (discount?.priceOff ?: 0.0),
            cycle = plan.cycle,
            tier = plan.tier
        )
}

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
