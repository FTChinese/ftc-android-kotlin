package com.ft.ftchinese.model.paywall

import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.stripesubs.StripePrice

data class CheckoutIntent(
    val kind: IntentKind,
    val message: String,
) {
    companion object {
        val vip = CheckoutIntent(
            kind = IntentKind.Forbidden,
            message = "VIP无需订阅",
        )

       val newMember = CheckoutIntent(
            kind = IntentKind.Create,
            message = "",
        )

        val intentUnknown = CheckoutIntent(
            kind = IntentKind.Forbidden,
            message = "仅支持新建订阅、续订、标准会员升级和购买额外订阅期限，不支持其他操作。\n当前会员购买方式未知，因此无法确定您可以执行哪些操作，请联系客服完善您的数据"
        )

        fun oneTimeRenewal(m: Membership): CheckoutIntent {
            if (m.beyondMaxRenewalPeriod()) {
                return CheckoutIntent(
                    kind = IntentKind.Forbidden,
                    message = "当前截止时间(${m.localizeExpireDate()})超出允许的最长续订期限，无法继续使用支付宝/微信再次购买"
                )
            }

            return CheckoutIntent(
                kind = IntentKind.Renew,
                message = "累加一个订阅周期"
            )
        }

        fun oneTimeDifferTier(target: Tier): CheckoutIntent {
            return when (target) {
                Tier.PREMIUM -> CheckoutIntent(
                    kind = IntentKind.Upgrade,
                    message = "马上升级高端会员，当前标准版剩余时间将在高端版结束后继续使用"
                )
                Tier.STANDARD -> CheckoutIntent(
                    kind = IntentKind.AddOn,
                    message = "购买的标准版订阅期限将在当前高端订阅结束后启用"
                )
            }
        }

        val autoRenewAddOn = CheckoutIntent(
            kind = IntentKind.AddOn,
            message = "当前订阅为自动续订，购买额外时长将在自动续订关闭并结束后启用",
        )

        val b2bAddOn = CheckoutIntent(
            kind = IntentKind.AddOn,
            message = "当前订阅来自企业版授权，个人购买的订阅时长将在授权取消或过期后启用",
        )

        @JvmStatic
        fun ofStripe(m: Membership, p: StripePrice): CheckoutIntent {
            if (m.vip) {
                return vip
            }

            if (m.isZero) {
                return newMember
            }

            return when (m.normalizedPayMethod) {
                PayMethod.ALIPAY,
                PayMethod.WXPAY -> CheckoutIntent(
                    kind = IntentKind.OneTimeToAutoRenew,
                    message = "使用Stripe转为自动续订，当前剩余时间将在新订阅失效后再次启用"
                )

                PayMethod.STRIPE -> if (m.tier == p.tier) {
                    if (m.cycle == p.periodCount.toCycle()) {
                        CheckoutIntent(
                            kind = IntentKind.Forbidden,
                            message = "自动续订不能重复订阅"
                        )
                    }  else {
                        CheckoutIntent(
                            kind = IntentKind.SwitchInterval,
                            message = "更改Stripe自动扣款周期，建议您订阅年度版更划算"
                        )
                    }
                } else {
                    when (p.tier) {
                        Tier.PREMIUM -> CheckoutIntent(
                            kind = IntentKind.Upgrade,
                            message = "升级高端会员，Stripe将自动调整您的扣款额度"
                        )
                        Tier.STANDARD -> CheckoutIntent(
                            kind = IntentKind.Downgrade,
                            message = "降级为标准版会员， Stripe将自动调整您的扣款额度"
                        )
                    }
                }

                PayMethod.APPLE -> CheckoutIntent(
                    kind = IntentKind.Forbidden,
                    message = "为避免重复订阅，苹果自动续订不能使用Stripe自动续订"
                )

                PayMethod.B2B -> CheckoutIntent(
                    kind = IntentKind.Forbidden,
                    message = "为避免重复订阅，企业版授权订阅不能使用Stripe自动续订"
                )

                else -> intentUnknown
            }
        }
    }
}
