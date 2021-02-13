package com.ft.ftchinese.ui.checkout

import android.content.Context
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.OrderKind
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.subscription.Discount
import com.ft.ftchinese.model.subscription.Price
import com.ft.ftchinese.ui.formatter.PriceStringBuilder
import org.jetbrains.anko.AnkoLogger

data class PayButtonParams(
    val selectedPayMethod: PayMethod,
    val orderKind: OrderKind,
    val price: Price,
    val discount: Discount? = null,
) : AnkoLogger {
    fun format(ctx: Context): String {
        return when (selectedPayMethod) {
            PayMethod.ALIPAY, PayMethod.WXPAY -> {
                // Ali/Wx pay button have two groups:
                // CREATE/RENEW/UPGRADE: 支付宝支付 ¥258.00 or 微信支付 ¥258.00
                // ADD_ON: 购买订阅期限
                if (orderKind == OrderKind.AddOn) {
                    ctx.getString(orderKind.stringRes)
                } else {
                    ctx.getString(
                        R.string.formatter_check_out,
                        ctx.getString(selectedPayMethod.stringRes),
                        PriceStringBuilder
                            .fromPrice(
                                price = price,
                                discount = discount,
                                withCycle = false
                            )
                            .build(ctx),
                    )
                }
            }
            // Stripe button has three groups:
            // CREATE: Stripe订阅
            // RENEW: 转为Stripe订阅
            // UPGRADE: Stripe订阅高端会员
            // SwitchCycle: Stripe变更订阅周期
            PayMethod.STRIPE -> {
                // if current pay method is not stripe.
                // If current pay method is stripe.
                when (orderKind) {
                    OrderKind.Create -> ctx.getString(selectedPayMethod.stringRes)
                    // Renew is used by alipay/wxpay switching to Stripe.
                    OrderKind.Renew -> ctx.getString(R.string.switch_to_stripe)
                    // This might be stripe standard upgrade, or ali/wx standard switching payment method.
                    OrderKind.Upgrade -> ctx.getString(selectedPayMethod.stringRes) + ctx.getString(price.tier.stringRes)
                    OrderKind.SwitchCycle -> ctx.getString(R.string.pay_brand_stripe) + ctx.getString(orderKind.stringRes)
                    OrderKind.AddOn -> "Stripe订阅不支持一次性购买"
                }
            }
            PayMethod.APPLE -> "无法处理苹果订阅"
            PayMethod.B2B -> "暂不支持企业订阅"
        }
    }
}
