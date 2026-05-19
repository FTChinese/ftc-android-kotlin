package com.ft.ftchinese.model.paywall

import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.ftcsubs.Price
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

        private val autoRenewAddOn = CheckoutIntent(
            kind = IntentKind.AddOn,
            message = "当前订阅为自动续订，购买额外时长将在自动续订关闭并结束后启用",
        )

        private val b2bAddOn = CheckoutIntent(
            kind = IntentKind.AddOn,
            message = "当前订阅来自企业版授权，个人购买的订阅时长将在授权取消或过期后启用",
        )

        /**
         * This method might be relocated to ui package
         * if we want to move hard-coded message to string resources.
         */
        @JvmStatic
        fun ofFtc(
            source: Membership,
            target: Price,
        ): CheckoutIntent {
            if (source.vip) {
                return vip
            }

            if (source.autoRenewOffExpired) {
                return newMember
            }

            return when (source.normalizedPayMethod) {
                PayMethod.ALIPAY,
                PayMethod.WXPAY -> if (source.tier == target.tier) {
                    if (source.beyondMaxRenewalPeriod()) {
                        return CheckoutIntent(
                            kind = IntentKind.Forbidden,
                            message = "到期时间(${source.localizeExpireDate()})超出允许的最长续订期限，无法继续使用支付宝/微信再次购买"
                        )
                    }

                    return CheckoutIntent(
                        kind = IntentKind.Renew,
                        message = "累加一个订阅周期"
                    )
                } else {
                    return when (target.tier) {
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

                PayMethod.STRIPE -> if (source.tier == target.tier) {
                    CheckoutIntent(
                        kind = IntentKind.Forbidden,
                        message = "当前为信用卡/借记卡自动续订，不能使用支付宝/微信重复购买同级别会员"
                    )
                } else {
                    CheckoutIntent(
                        kind = IntentKind.Forbidden,
                        message = "当前为信用卡/借记卡自动续订，请继续使用信用卡/借记卡管理升级或降级"
                    )
                }

                PayMethod.APPLE -> CheckoutIntent(
                    kind = IntentKind.Forbidden,
                    message = "当前订阅来自苹果内购，请在苹果设备上管理订阅"
                )

                PayMethod.B2B -> if (source.tier == Tier.STANDARD && target.tier == Tier.PREMIUM) {
                    CheckoutIntent(
                        kind = IntentKind.Forbidden,
                        message = "当前订阅来自企业版授权，升级高端订阅请联系您所属机构的管理人员"
                    )
                } else {
                    b2bAddOn
                }

                else -> intentUnknown
            }
        }

        @JvmStatic
        fun ofStripe(
            source: Membership,
            target: StripePrice,
        ): CheckoutIntent {
            if (source.vip) {
                return vip
            }

            if (source.autoRenewOffExpired) {
                return newMember
            }

            return when (source.normalizedPayMethod) {
                PayMethod.ALIPAY,
                PayMethod.WXPAY -> CheckoutIntent(
                    kind = IntentKind.OneTimeToAutoRenew,
                    message = "使用信用卡/借记卡转为自动续订，当前剩余时间将在新订阅失效后再次启用"
                )

                PayMethod.STRIPE -> if (source.tier == target.tier) {
                    val pendingChange = source.pendingStripeChange
                    if (pendingChange?.isDowngrade == true && !pendingChange.targets(target.tier)) {
                        CheckoutIntent(
                            kind = IntentKind.CancelScheduledChange,
                            message = "当前仍保留${target.tier.label()}权益；取消已安排的降级后，下次续订将继续按${target.tier.label()}扣款"
                        )
                    } else if (source.cycle == target.periodCount.toCycle()) {
                        // Campaign coupons are applied when creating or changing a
                        // Stripe plan. The catalog should not let an active same-tier
                        // auto-renewal look like a second purchase path.
                        CheckoutIntent(
                            kind = IntentKind.Forbidden,
                            message = "自动续订不能重复订阅"
                        )
                    } else {
                        CheckoutIntent(
                            kind = IntentKind.SwitchInterval,
                            message = "更改信用卡/借记卡自动扣款周期，建议您订阅年度版更划算"
                        )
                    }
                } else {
                    if (source.pendingStripeChange?.targets(target.tier) == true) {
                        return CheckoutIntent(
                            kind = IntentKind.Forbidden,
                            message = "已安排下次续订起切换为${target.tier.label()}"
                        )
                    }
                    when (target.tier) {
                        Tier.PREMIUM -> CheckoutIntent(
                            kind = IntentKind.Upgrade,
                            message = "升级高端会员，信用卡/借记卡自动续订将调整您的扣款额度"
                        )
                        Tier.STANDARD -> if (source.autoRenew) {
                            CheckoutIntent(
                                kind = IntentKind.Downgrade,
                                message = "当前高端会员权益保留到本周期结束，下次续订起切换为标准会员"
                            )
                        } else {
                            CheckoutIntent(
                                kind = IntentKind.Forbidden,
                                message = "自动续订已关闭，到期后不再续订；如需下周期切换方案，请先开启自动续订"
                            )
                        }
                    }
                }

                PayMethod.APPLE -> CheckoutIntent(
                    kind = IntentKind.Forbidden,
                    message = "为避免重复订阅，苹果自动续订不能使用信用卡/借记卡自动续订"
                )

                PayMethod.B2B -> CheckoutIntent(
                    kind = IntentKind.Forbidden,
                    message = "为避免重复订阅，企业版授权订阅不能使用信用卡/借记卡自动续订"
                )

                else -> intentUnknown
            }
        }
    }
}

private fun Tier?.label(): String {
    return when (this) {
        Tier.STANDARD -> "标准会员"
        Tier.PREMIUM -> "高端会员"
        null -> "当前会员"
    }
}
