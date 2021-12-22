package com.ft.ftchinese.model.paywall

import com.ft.ftchinese.model.enums.Edition
import com.ft.ftchinese.model.enums.OrderKind
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.reader.Membership

data class OrderIntent(
    val kind: OrderKind?,
    val message: String?
)

data class PaymentChoices(
    val warning: String? = null,
    val aliPay: OrderIntent,
    val wxPay: OrderIntent,
    val stripe: OrderIntent
) {

    fun findOrderIntent(m: PayMethod): OrderIntent? {
        return when (m) {
            PayMethod.ALIPAY -> aliPay
            PayMethod.WXPAY -> wxPay
            PayMethod.STRIPE -> stripe
            else -> null
        }
    }

    fun isPayMethodEnabled(m: PayMethod): Boolean {
        return findOrderIntent(m)?.kind != null
    }

    companion object {

        private const val vipForbidden = "VIP无需订阅"
        private const val oneTimeRenewal = "累加一个订阅周期"
        private const val oneTimeStd2Prm = "马上升级高端会员，当前标准版剩余时间将在高端版结束后继续使用"
        private const val oneTimePrm2StdAddOn = "高端会员购买的标准版订阅期限将在当前订阅结束后启用"
        private const val oneTime2Stripe = "使用Stripe转为自动续订，当前剩余时间将在新订阅失效后再次启用。"
        private const val stripeDuplicate = "自动续订不能重复订阅"
        private const val autoRenewAddOn = "当前订阅为自动续订，购买额外时长将在自动续订关闭并结束后启用"

        @JvmStatic
        private fun ofVip(): PaymentChoices {
            return PaymentChoices(
                warning = vipForbidden,
                aliPay = OrderIntent(
                    kind = null,
                    message = null
                ),
                wxPay = OrderIntent(
                    kind = null,
                    message = null,
                ),
                stripe = OrderIntent(
                    kind = null,
                    message = null,
                )
            )
        }

        @JvmStatic
        private fun ofNewMember(): PaymentChoices {
            return PaymentChoices(
                warning = null,
                aliPay = OrderIntent(
                    kind = OrderKind.Create,
                    message = null
                ),
                wxPay = OrderIntent(
                    kind = OrderKind.Create,
                    message = null,
                ),
                stripe = OrderIntent(
                    kind = OrderKind.Create,
                    message = null,
                )
            )
        }

        @JvmStatic
        private fun ofOneTimeBeyondRenewal(expTime: String): PaymentChoices {
            return PaymentChoices(
                warning = "剩余时间(${expTime})超出允许的最长续订期限，无法继续使用支付宝/微信再次购买",
                aliPay = OrderIntent(
                    kind = null,
                    message = null,
                ),
                wxPay = OrderIntent(
                    kind = null,
                    message = null
                ),
                stripe = OrderIntent(
                    kind = OrderKind.Create,
                    message = oneTime2Stripe
                )
            )
        }

        @JvmStatic
        private fun ofOneTimeRenewal(): PaymentChoices {
            return PaymentChoices(
                warning = null,
                aliPay = OrderIntent(
                    kind = OrderKind.Renew,
                    message = "使用支付宝购买$oneTimeRenewal",
                ),
                wxPay = OrderIntent(
                    kind = OrderKind.Renew,
                    message = "使用微信购买$oneTimeRenewal"
                ),
                stripe = OrderIntent(
                    kind = OrderKind.Create,
                    message = oneTime2Stripe
                )
            )
        }

        @JvmStatic
        private fun ofOneTimeUpgrade(): PaymentChoices {
            return PaymentChoices(
                warning = null,
                aliPay = OrderIntent(
                    kind = OrderKind.Upgrade,
                    message = "使用支付宝购买$oneTimeStd2Prm",
                ),
                wxPay = OrderIntent(
                    kind = OrderKind.Upgrade,
                    message = "使用微信购买$oneTimeStd2Prm",
                ),
                stripe = OrderIntent(
                    kind = OrderKind.Create,
                    message = oneTime2Stripe,
                )
            )
        }

        @JvmStatic
        private fun ofOneTimePrm2Std(): PaymentChoices {
            return PaymentChoices(
                warning = null,
                aliPay = OrderIntent(
                    kind = OrderKind.AddOn,
                    message = oneTimePrm2StdAddOn
                ),
                wxPay = OrderIntent(
                    kind = OrderKind.AddOn,
                    message = oneTimePrm2StdAddOn
                ),
                stripe = OrderIntent(
                    kind = OrderKind.Create,
                    message = "Stripe订阅优先级更高，为避免覆盖当前高端订阅，建议到期后再使用Stripe订阅"
                )
            )
        }

        @JvmStatic
        private fun ofStripeSameEdition(): PaymentChoices {
            return PaymentChoices(
                warning = null,
                aliPay = OrderIntent(
                    kind = OrderKind.AddOn,
                    message = autoRenewAddOn
                ),
                wxPay = OrderIntent(
                    kind = OrderKind.AddOn,
                    message = autoRenewAddOn
                ),
                stripe = OrderIntent(
                    kind = null,
                    message = stripeDuplicate
                )
            )
        }

        @JvmStatic
        private fun ofStripeStd2Prm(): PaymentChoices {
            return PaymentChoices(
                warning = null,
                aliPay = OrderIntent(
                    kind = null,
                    message = "自动续订不能使用支付宝升级高端版",
                ),
                wxPay = OrderIntent(
                    kind = null,
                    message = "自动续订不能使用微信支付升级高端版"
                ),
                stripe = OrderIntent(
                    kind = OrderKind.Upgrade,
                    message = "升级高端会员后Stripe将自动调整您的扣款额度"
                )
            )
        }

        @JvmStatic
        private fun ofStripeSwitchCycle(): PaymentChoices {
            return PaymentChoices(
                warning = null,
                aliPay = OrderIntent(
                    kind = OrderKind.AddOn,
                    message = autoRenewAddOn,
                ),
                wxPay = OrderIntent(
                    kind = OrderKind.AddOn,
                    message = autoRenewAddOn,
                ),
                stripe = OrderIntent(
                    kind = OrderKind.SwitchCycle,
                    message = "更改Stripe自动扣款周期，建议您订阅年度版更划算。"
                )
            )
        }

        @JvmStatic
        private fun ofAppleUpgrade(): PaymentChoices {
            return PaymentChoices(
                warning = "当前标准会员会员来自苹果内购，升级高端会员需要在您的苹果设备上，使用原有苹果账号登录后，在FT中文网APP内操作",
                aliPay = OrderIntent(
                    kind = null,
                    message = null,
                ),
                wxPay = OrderIntent(
                    kind = null,
                    message = null,
                ),
                stripe = OrderIntent(
                    kind = null,
                    message = null,
                )
            )
        }

        @JvmStatic
        private fun ofAppleAddOn(): PaymentChoices {
            return PaymentChoices(
                warning = "当前会员来自苹果内购",
                aliPay = OrderIntent(
                    kind = OrderKind.AddOn,
                    message = autoRenewAddOn,
                ),
                wxPay = OrderIntent(
                    kind = OrderKind.AddOn,
                    message = autoRenewAddOn,
                ),
                stripe = OrderIntent(
                    kind = null,
                    message = "苹果自动续订不能使用Stripe订阅"
                )
            )
        }

        @JvmStatic
        fun newInstance(m: Membership, e: Edition): PaymentChoices {
            if (m.vip) {
                return ofVip()
            }

            // Invalid stripe subscription is treated as not having a membership.
            if (m.tier == null) {
                return ofNewMember()
            }

            if (m.expired || m.isInvalidStripe) {
                return ofNewMember()
            }

            when (m.normalizedPayMethod) {
                PayMethod.ALIPAY, PayMethod.WXPAY -> {
                    if (m.tier == e.tier) {
                        // For alipay and wxpay, allow user to renew if
                        // remaining days not exceeding 3 years.
                        return if (m.beyondMaxRenewalPeriod()) {
                            ofOneTimeBeyondRenewal(m.localizeExpireDate())
                        } else {
                            ofOneTimeRenewal()
                        }
                    }

                    // Current membership's tier differs from the selected one.
                    // Allowed actions depends on what is being selected.

                    return when (e.tier) {
                        Tier.PREMIUM -> {
                            ofOneTimeUpgrade()
                        }
                        Tier.STANDARD -> {
                            ofOneTimePrm2Std()
                        }
                    }
                }
                PayMethod.STRIPE -> {
                    // This includes 3 cases:
                    // Premium to Premium
                    // Standard Monthly to Standard Monthly
                    // Standard Yearly to Standard Yearly
                    // For same edition, Stripe is not allowed to use again.
                    if (m.tier == e.tier && m.cycle == e.cycle) {
                        return ofStripeSameEdition()
                    }

                    // If current subscription is Stripe standard edition,
                    // we have to handle some edge cases
                    // depending on selected target.
                    if (m.tier == Tier.STANDARD) {
                        return when (e.tier) {
                            // If selected premium, this is upgrade.
                            // It could only use Stripe for upgrade.
                            Tier.PREMIUM -> ofStripeStd2Prm()
                            // If selected standard, the billing cycle
                            // must be different.
                            Tier.STANDARD -> ofStripeSwitchCycle()
                        }
                    }
                }
                PayMethod.APPLE -> {
                    // Current standard is trying to upgrade to premium.
                    return if (m.tier == Tier.STANDARD && e.tier == Tier.PREMIUM) {
                        ofAppleUpgrade()
                    } else {
                        ofAppleAddOn()
                    }
                }
                PayMethod.B2B -> {
                    return PaymentChoices(
                        warning = "您目前使用的是企业订阅授权，续订或升级请联系您所属机构的管理人员",
                        aliPay = OrderIntent(
                            kind = null,
                            message = null,
                        ),
                        wxPay = OrderIntent(
                            kind = null,
                            message = null,
                        ),
                        stripe = OrderIntent(
                            kind = null,
                            message = null,
                        )
                    )
                }
            }

            return PaymentChoices(
                warning = "仅支持新建订阅、续订、标准会员升级和购买额外订阅期限，不支持其他操作。\n当前会员购买方式未知，因此无法确定您可以执行哪些操作，请联系客服完善您的数据",
                aliPay = OrderIntent(
                    kind = null,
                    message = null,
                ),
                wxPay = OrderIntent(
                    kind = null,
                    message = null,
                ),
                stripe = OrderIntent(
                    kind = null,
                    message = null,
                )
            )
        }
    }
}


