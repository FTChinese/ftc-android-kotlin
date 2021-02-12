package com.ft.ftchinese.ui.checkout

import android.content.Context
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.OrderKind
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.subscription.Discount
import com.ft.ftchinese.model.subscription.Price
import com.ft.ftchinese.ui.formatter.PriceStringBuilder

data class PayButtonParams(
    val payMethod: PayMethod,
    val orderKind: OrderKind,
    val price: Price,
    val discount: Discount? = null,
) {
    fun format(ctx: Context): String {
        when (payMethod) {
            PayMethod.ALIPAY, PayMethod.WXPAY -> {
                // Ali/Wx pay button have two groups:
                // CREATE/RENEW/UPGRADE: 支付宝支付 ¥258.00 or 微信支付 ¥258.00
                // ADD_ON: 购买订阅期限
                if (orderKind == OrderKind.AddOn) {
                    return ctx.getString(orderKind.stringRes)
                }

                return ctx.getString(
                    R.string.formatter_check_out,
                    ctx.getString(payMethod.stringRes),
                    PriceStringBuilder
                        .fromPrice(
                            price = price,
                            discount = discount,
                            withCycle = false
                        )
                        .build(ctx),
                )
            }
            // Stripe button has three groups:
            // CREATE: Stripe订阅
            // UPGRADE: Stripe订阅升级
            // SwitchCycle: Stripe变更订阅周期
            PayMethod.STRIPE -> {
                when (orderKind) {
                    OrderKind.Create -> return ctx.getString(payMethod.stringRes)
                    OrderKind.Upgrade -> return ctx.getString(payMethod.stringRes) + ctx.getString(orderKind.stringRes) + ctx.getString(price.tier.stringRes)
                    OrderKind.SwitchCycle -> ctx.getString(R.string.pay_brand_stripe) + ctx.getString(orderKind.stringRes)
                }
            }
        }

        return ctx.getString(R.string.check_out)
    }
}
