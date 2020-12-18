package com.ft.ftchinese.ui.pay

import android.content.Context
import android.os.Parcelable
import com.ft.ftchinese.R
import com.ft.ftchinese.model.subscription.Checkout
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

data class UpgradePreview(
    val amount: String = "",
    val price: String = "",
    val balance: String = "",
    val conversion: String = "",
    val confirmUpgrade: String = ""
)

fun upgradePreviewUI(ctx: Context, co: Checkout): UpgradePreview {
    return UpgradePreview(
        amount = formatPrice(
            ctx = ctx,
            currency = co.payable.currency,
            price = co.payable.amount
        ),
        price = ctx.getString(
            R.string.premium_price,
            formatPrice(
                ctx = ctx,
                currency = co.item.plan.currency,
                price = co.item.plan.price,
            )
        ),
        balance = ctx.getString(
            R.string.account_balance,
            formatPrice(
                ctx = ctx,
                currency = co.payable.currency,
                price = co.wallet.balance
            )
        ),
        conversion = if (!co.isFree) {
            "升级首先使用当前余额抵扣，不足部分需要另行支付"
        } else {
            ctx.getString(
                R.string.balance_conversion,
                co.duration.cycleCount,
                co.duration.extraDays
            )
        },
        confirmUpgrade = if (co.isFree) {
            ctx.getString(R.string.direct_upgrade)
        } else {
            ctx.getString(R.string.confirm_upgrade)
        }
    )
}
