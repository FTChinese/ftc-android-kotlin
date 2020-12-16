package com.ft.ftchinese.ui.pay

import android.content.Context
import android.os.Parcelable
import com.ft.ftchinese.model.subscription.OrderKind
import com.ft.ftchinese.model.subscription.Plan
import com.ft.ftchinese.model.subscription.StripePrice
import com.ft.ftchinese.ui.base.formatPrice
import com.ft.ftchinese.ui.base.formatTierCycle
import kotlinx.parcelize.Parcelize

// Passed to CartItemFragment
@Parcelize
data class CartItem(
        val price: String,
        val name: String
) : Parcelable

// Data passed to StripSubActivity
@Parcelize
data class StripeCheckout(
    val kind: OrderKind,
    val price: StripePrice
) : Parcelable

fun stripeCartItem(ctx: Context, price: StripePrice): CartItem {
    return CartItem(
        price = formatPrice(
            ctx = ctx,
            currency = price.currency,
            price = price.humanAmount()
        ),
        name = formatTierCycle(
            ctx = ctx,
            tier = price.tier,
            cycle = price.cycle
        )
    )
}

// Passed to CheckoutActivity.
@Parcelize
data class FtcCheckout(
    val kind: OrderKind,
    val plan: Plan
) : Parcelable

fun ftcCartItem(ctx: Context, plan: Plan): CartItem {
    return CartItem(
        price = formatPrice(
            ctx = ctx,
            currency = plan.currency,
            price = plan.price
        ),
        name = formatTierCycle(
            ctx = ctx,
            tier = plan.tier,
            cycle = plan.cycle
        )
    )
}
