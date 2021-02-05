package com.ft.ftchinese.ui.checkout

import android.content.Context
import android.os.Parcelable
import com.ft.ftchinese.R
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.subscription.*
import com.ft.ftchinese.ui.base.formatPrice
import com.ft.ftchinese.ui.base.getCurrencySymbol
import kotlinx.parcelize.Parcelize

data class CheckoutIntent(
    val orderKind: OrderKind?,
    val payMethods: List<PayMethod>,
    val warning: String,
) {
    val permitAliPay: Boolean
        get() = payMethods.contains(PayMethod.ALIPAY)

    val permitWxPay: Boolean
        get() = payMethods.contains(PayMethod.WXPAY)

    val permitStripe: Boolean
        get() = payMethods.contains(PayMethod.STRIPE)
}

fun buildCheckoutIntent(m: Membership, e: Edition): CheckoutIntent {
    if (m.expired()) {
        return CheckoutIntent(
            orderKind = OrderKind.CREATE,
            payMethods = listOf(PayMethod.ALIPAY, PayMethod.WXPAY, PayMethod.STRIPE),
            warning = "",
        )
    }

    // Renewal
    if (m.tier == e.tier) {
       when (m.payMethod) {
           // For alipay and wxpay, allow user to renew if
           // remaining days not exceeding 3 years.
           PayMethod.ALIPAY, PayMethod.WXPAY -> {
               // Remaining days exceed 3 years
               if (!m.withinAliWxRenewalPeriod()) {
                   return CheckoutIntent(
                       orderKind = OrderKind.RENEW,
                       payMethods = listOf(),
                       warning = "剩余时间超出允许的最长续订期限",
                   )
               }
               // Ali/Wx member can renew via Ali/Wx, or Stripe with remaining days put to reserved state.
               return CheckoutIntent(
                   orderKind = OrderKind.RENEW,
                   payMethods = listOf(PayMethod.ALIPAY, PayMethod.STRIPE),
                   warning = "您当前会员通过支付宝/微信购买，如果选择Stripe订阅，请阅读下方注意事项"
               )
           }
           // For stripe, apple, b2b, the purchase is treated as addon.
           PayMethod.STRIPE, PayMethod.APPLE -> {
               return CheckoutIntent(
                   orderKind = OrderKind.ADD_ON,
                   payMethods = listOf(PayMethod.ALIPAY, PayMethod.WXPAY),
                   warning = "Stripe/苹果订阅会员请阅读下方注意事项"
               )
           }
           PayMethod.B2B -> {
               return CheckoutIntent(
                   orderKind = OrderKind.RENEW,
                   payMethods = listOf(),
                   warning = "您目前使用的是企业订阅授权，延长订阅期限请联系您所属机构的管理人员"
               )
           }
       }
    }

    // Upgrade
    if (e.tier == Tier.PREMIUM) {
        when (m.payMethod) {
            // For ali/Wx purchased membership to upgrade,
            // they can use ali, wx or stripe.
            // current remaining days will be transferred to addon.
            PayMethod.ALIPAY, PayMethod.WXPAY -> {
                return CheckoutIntent(
                    orderKind = OrderKind.UPGRADE,
                    payMethods = listOf(PayMethod.ALIPAY, PayMethod.WXPAY, PayMethod.STRIPE),
                    warning = "升级高端会员将即可启用，标准版的剩余时间将在高端版失效后继续使用",
                )
            }
            // For Stripe
            PayMethod.STRIPE -> {
                return CheckoutIntent(
                    orderKind = OrderKind.UPGRADE,
                    payMethods = listOf(PayMethod.STRIPE),
                    warning = "Stripe订阅升级高端版会自动调整您的扣款额度",
                )
            }
            // If current membership is purchased via Apple,
            // ask user to performing upgrade back on iOS devices.
            PayMethod.APPLE -> {
                return CheckoutIntent(
                    orderKind = OrderKind.UPGRADE,
                    payMethods = listOf(),
                    warning = "苹果内购的订阅升级高端版需要在您的苹果设备上，使用您的原有苹果账号登录后，在FT中文网APP内操作",
                )
            }
            // If current membership is granted by b2b,
            // ask users to contact their org's admin
            PayMethod.B2B -> {
                return CheckoutIntent(
                    orderKind = OrderKind.UPGRADE,
                    payMethods = listOf(),
                    warning = "您目前使用的是企业订阅授权，升级到高端会员请联系您所属机构管理人员"
                )
            }
        }
    }

    return CheckoutIntent(
        orderKind = null,
        payMethods = listOf(),
        warning = "仅支持新建订阅、续订和标准会员升级高会员，不支持其他操作。",
    )
}

data class Payable(
    val currencySymbol: String,
    val amount: String,
    val cycle: String
)

data class Cart(
    val planName: String,
    val originalPrice: String?,
    val payable: Payable?,
)

fun buildFtcCart(ctx: Context, plan: Plan): Cart {

    val hasDiscount = plan.discount.isValid()

    return Cart(
        planName = ctx.getString(plan.tier.stringRes),
        originalPrice = if (hasDiscount) {
            formatPrice(
                ctx = ctx,
                currency = plan.currency,
                price = plan.price
            )
        } else null,
        payable = Payable(
            currencySymbol = getCurrencySymbol(plan.currency),
            amount = if (hasDiscount) {
                "${plan.price - (plan.discount.priceOff ?: 0.0)}"
            } else {
                "${plan.price}"
            },
            cycle = ctx.getString(plan.cycle.stringRes)
        )
    )
}

fun buildStripeCart(ctx: Context, price: StripePrice): Cart {
    return Cart(
        planName = ctx.getString(price.tier.stringRes),
        originalPrice = null,
        payable = Payable(
            currencySymbol = getCurrencySymbol(price.currency),
            amount = "${price.unitAmount}",
            cycle = ctx.getString(price.cycle.stringRes)
        )
    )
}

fun getOrderKindText(ctx: Context, kind: OrderKind): String {
    val strRes = when (kind) {
        OrderKind.RENEW -> R.string.title_renewal
        OrderKind.UPGRADE -> R.string.title_upgrade
        else -> R.string.title_checkout
    }

    return ctx.getString(strRes)
}

