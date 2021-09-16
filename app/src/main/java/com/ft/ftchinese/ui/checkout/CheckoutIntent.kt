package com.ft.ftchinese.ui.checkout

import android.content.Context
import com.ft.ftchinese.R
import com.ft.ftchinese.model.price.Edition
import com.ft.ftchinese.model.enums.OrderKind
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.ftcsubs.CheckoutItem
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.ui.formatter.PriceStringBuilder

data class CheckoutIntents(
    val intents: List<CheckoutIntent>,
    val warning: String,
) {
    private val payMethods: List<PayMethod>
        get() = intents.flatMap { it.payMethods }

    val payMethodsState: PaymentMethodsEnabled
        get() = PaymentMethodsEnabled(
            alipay = payMethods.contains(PayMethod.ALIPAY),
            wechat = payMethods.contains(PayMethod.WXPAY),
            stripe = payMethods.contains(PayMethod.STRIPE)
        )

    /**
     * Find the intent containing the specified payment method.
     */
    fun findIntent(method: PayMethod): CheckoutIntent? {
        if (intents.isEmpty()) {
            return null
        }

        return intents.find { it.payMethods.contains(method) }
    }

    companion object {
        @JvmStatic
        fun newInstance(m: Membership, e: Edition): CheckoutIntents {
            if (m.vip) {
                return CheckoutIntents(
                    intents = listOf(),
                    warning = "VIP无需订阅"
                )
            }

            if (m.expired) {
                return CheckoutIntents(
                    intents = listOf(CheckoutIntent(
                        orderKind = OrderKind.Create,
                        payMethods = listOf(PayMethod.ALIPAY, PayMethod.WXPAY, PayMethod.STRIPE),
                    )),
                    warning = "",
                )
            }

            // Invalid stripe subscription is treated as not having a membership.
            if (m.isInvalidStripe) {
                return CheckoutIntents(
                    intents = listOf(CheckoutIntent(
                        orderKind = OrderKind.Create,
                        payMethods = listOf(PayMethod.ALIPAY, PayMethod.WXPAY, PayMethod.STRIPE),
                    )),
                    warning = "",
                )
            }

            when (m.normalizedPayMethod) {
                PayMethod.ALIPAY, PayMethod.WXPAY -> {
                    // Renewal
                    if (m.tier == e.tier) {
                        // For alipay and wxpay, allow user to renew if
                        // remaining days not exceeding 3 years.
                        if (!m.withinAliWxRenewalPeriod()) {
                            return CheckoutIntents(
                                intents = listOf(CheckoutIntent(
                                    orderKind = OrderKind.Create,
                                    payMethods = listOf(PayMethod.STRIPE)
                                )),
                                warning = "* 剩余时间(${m.localizeExpireDate()})超出允许的最长续订期限，无法继续使用支付宝/微信再次购买；\n* 可以转为Stripe订阅，当前剩余时间将在新订阅失效后再此启用。",
                            )
                        }
                        // Ali/Wx member can renew via Ali/Wx, or Stripe with remaining days put to reserved state.
                        return CheckoutIntents(
                            intents = listOf(CheckoutIntent(
                                orderKind = OrderKind.Renew,
                                payMethods = listOf(PayMethod.ALIPAY, PayMethod.WXPAY),
                            ), CheckoutIntent(
                                orderKind = OrderKind.Create,
                                payMethods = listOf(PayMethod.STRIPE)
                            )),
                            warning = "* 选择支付宝/微信购买累加一个订阅周期。\n* 选择Stripe订阅，当前剩余订阅时间将在Stripe订阅结束后继续使用。"
                        )
                    }

                    // Current membership's tier differs from the selected one.
                    // Allowed actions depends on what is being selected.
                    when (e.tier) {
                        // This is an Upgrade action if standard user selected premium product.
                        Tier.PREMIUM -> {
                            // For stripe this is switching payment method.
                            return CheckoutIntents(
                                intents = listOf(CheckoutIntent(
                                    orderKind = OrderKind.Upgrade,
                                    payMethods = listOf(PayMethod.ALIPAY, PayMethod.WXPAY)
                                ), CheckoutIntent(
                                    orderKind = OrderKind.Create,
                                    payMethods = listOf(PayMethod.STRIPE)
                                )),
                                warning = "* 使用支付宝/微信升级高端会员即刻启用，当前剩余时间将在高端版结束后继续使用；\n* 或转为Stripe订阅，当前剩余时间将在Stripe失效后重新启用",
                            )
                        }
                        // A premium could buy standard as AddOns.
                        Tier.STANDARD -> {
                            return CheckoutIntents(
                                intents = listOf(
                                    CheckoutIntent(
                                        orderKind = OrderKind.AddOn,
                                        payMethods = listOf(PayMethod.ALIPAY, PayMethod.WXPAY),
                                    ),
                                    CheckoutIntent(
                                        orderKind = OrderKind.Create,
                                        payMethods = listOf(PayMethod.STRIPE)
                                    )
                                ),
                                warning = "您当前是高端会员\n* 选择支付宝/微信将购买新的标准版订阅期限，在高端版结束后启用。\n* 选择Stripe将转为订阅模式，当前会员剩余时间在订阅取消后继续使用。"
                            )
                        }
                    }
                }
                PayMethod.STRIPE -> {
                    // If user is a premium, whatever selected should be treated as AddOn.
                    when (m.tier) {
                        Tier.PREMIUM -> {
                            return CheckoutIntents(
                                intents = listOf(CheckoutIntent(
                                    orderKind = OrderKind.AddOn,
                                    payMethods = listOf(PayMethod.ALIPAY, PayMethod.WXPAY),
                                )),
                                warning = "当前订阅**高端会员/年**来自Stripe自动续订，为保障您的权益，仅可使用支付宝/微信购买订阅期限，在Stripe订阅结束后启用。",
                            )
                        }
                        // Currently subscribed to standard edition.
                        Tier.STANDARD -> {
                            when (e.tier) {
                                // If selected premium, this is upgrade.
                                // It could use Stripe for upgrade.
                                Tier.PREMIUM -> {
                                    return CheckoutIntents(
                                        intents = listOf(
                                            CheckoutIntent(
                                                orderKind = OrderKind.Upgrade,
                                                payMethods = listOf(PayMethod.STRIPE),
                                            )),
                                        warning = "当前订阅标准会员来自Stripe自动续订，升级高端会员后Stripe将自动调整您的扣款额度",
                                    )
                                }
                                // If selected standard...
                                Tier.STANDARD -> {
                                    // For same cycle, it could only be add-on
                                    if (m.cycle == e.cycle) {
                                        return CheckoutIntents(
                                            intents = listOf(CheckoutIntent(
                                                orderKind = OrderKind.AddOn,
                                                payMethods = listOf(PayMethod.ALIPAY, PayMethod.WXPAY),
                                            )),
                                            warning = "当前标准会员来自Stripe订阅：\n* 选择支付宝/微信购买一次性订阅期限，在Stripe订阅结束后启用。\n* 为避免产生重复订阅同一产品，不能使用Stripe支付。",
                                        )
                                    }

                                    // Same tier but different cycle.
                                    return CheckoutIntents(
                                        intents = listOf(
                                            // For Ali/Wx, it's add-on
                                            CheckoutIntent(
                                                orderKind = OrderKind.AddOn,
                                                payMethods = listOf(PayMethod.ALIPAY, PayMethod.WXPAY),
                                            ),
                                            // for Stripe, it's switching cycle.
                                            CheckoutIntent(
                                                orderKind = OrderKind.SwitchCycle,
                                                payMethods = listOf(PayMethod.STRIPE)
                                            )
                                        ),
                                        warning = "当前标准会员来自Stripe订阅：\n* 选择支付宝/微信购买一次性订阅期限，在Stripe订阅结束后启用。\n* 选择Stripe支付更改自动扣款周期。\n自动订阅建议您订阅年度版更划算。"
                                    )
                                }
                            }
                        }
                    }
                }
                PayMethod.APPLE -> {
                    if (m.tier == Tier.STANDARD && e.tier == Tier.PREMIUM) {
                        return CheckoutIntents(
                            intents = listOf(CheckoutIntent(
                                orderKind = OrderKind.Upgrade,
                                payMethods = listOf(),
                            )),
                            warning = "当前标准会员会员来自苹果内购，升级高端会员需要在您的苹果设备上，使用原有苹果账号登录后，在FT中文网APP内操作"
                        )
                    }
                    // All other options are add-ons
                    return CheckoutIntents(
                        intents = listOf(CheckoutIntent(
                            orderKind = OrderKind.AddOn,
                            payMethods = listOf(PayMethod.ALIPAY, PayMethod.WXPAY),
                        )),
                        warning = "当前标准会员会员来自苹果内购：\n* 选择支付宝/微信购买一次性订阅期限，在苹果订阅结束后启用。\n* 订阅模式不能再选择Stripe支付",
                    )
                }
                PayMethod.B2B -> {
                    return CheckoutIntents(
                        intents = listOf(),
                        warning = "您目前使用的是企业订阅授权，续订或升级请联系您所属机构的管理人员"
                    )
                }
            }

            return CheckoutIntents(
                intents = listOf(),
                warning = "仅支持新建订阅、续订、标准会员升级和购买额外订阅期限，不支持其他操作。\n当前会员购买方式未知，因此无法确定您可以执行哪些操作，请联系客服完善您的数据",
            )
        }
    }
}

data class CheckoutIntent(
    val orderKind: OrderKind,
    val payMethods: List<PayMethod>,
)

/**
 * Collects all data required for final payment after
 * user selected a payment method.
 */
data class PaymentIntent (
    val item: CheckoutItem,
    val orderKind: OrderKind,
    val payMethod: PayMethod
) {
    fun composeBtnText(ctx: Context): String {
        return when (payMethod) {
            PayMethod.ALIPAY, PayMethod.WXPAY -> {
                // Ali/Wx pay button have two groups:
                // CREATE/RENEW/UPGRADE: 支付宝支付 ¥258.00 or 微信支付 ¥258.00
                // ADD_ON: 购买订阅期限
                if (orderKind == OrderKind.AddOn) {
                    ctx.getString(orderKind.stringRes)
                } else {
                    ctx.getString(
                        R.string.formatter_check_out,
                        ctx.getString(payMethod.stringRes),
                        PriceStringBuilder
                            .fromPrice(
                                price = item.price,
                                discount = item.discount,
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
                    OrderKind.Create -> ctx.getString(payMethod.stringRes)
                    // Renew is used by alipay/wxpay switching to Stripe.
                    OrderKind.Renew -> ctx.getString(R.string.switch_to_stripe)
                    // This might be stripe standard upgrade, or ali/wx standard switching payment method.
                    OrderKind.Upgrade -> ctx.getString(payMethod.stringRes) + ctx.getString(item.price.tier.stringRes)
                    OrderKind.SwitchCycle -> ctx.getString(R.string.pay_brand_stripe) + ctx.getString(orderKind.stringRes)
                    OrderKind.AddOn -> "Stripe订阅不支持一次性购买"
                }
            }
            PayMethod.APPLE -> "无法处理苹果订阅"
            PayMethod.B2B -> "暂不支持企业订阅"
        }
    }
}
