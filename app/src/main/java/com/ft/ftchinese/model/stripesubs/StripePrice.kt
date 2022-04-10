package com.ft.ftchinese.model.stripesubs

import android.os.Parcelable
import com.ft.ftchinese.model.enums.Edition
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.enums.PriceKind
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.ftcsubs.YearMonthDay
import com.ft.ftchinese.model.paywall.CheckoutIntent
import com.ft.ftchinese.model.paywall.IntentKind
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.serializer.DateTimeAsStringSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.threeten.bp.ZonedDateTime

@Parcelize
@Serializable
data class StripePrice(
    val id: String,
    val active: Boolean,
    val currency: String,
    val isIntroductory: Boolean = false,
    val kind: PriceKind = PriceKind.Recurring,
    val liveMode: Boolean,
    val nickname: String,
    val productId: String = "",
    val periodCount: YearMonthDay = YearMonthDay(),
    val tier: Tier,
    val unitAmount: Int,
    @Serializable(with = DateTimeAsStringSerializer::class)
    val startUtc: ZonedDateTime? = null,
    @Serializable(with = DateTimeAsStringSerializer::class)
    val endUtc: ZonedDateTime? = null,
    val created: Int = 0,
) : Parcelable {

    val edition: Edition
        get() = Edition(
            tier = tier,
            cycle = periodCount.toCycle()
        )

    fun isValid(): Boolean {
        if (kind == PriceKind.Recurring) {
            return true
        }

        if (startUtc == null || endUtc == null) {
            return true
        }

        val now = ZonedDateTime.now()

        if (now.isBefore(startUtc) || now.isAfter(endUtc)) {
            return false
        }

        return true
    }

    val moneyAmount: Double
        get() = unitAmount
            .toBigDecimal()
            .divide(
                100.toBigDecimal()
            )
            .toDouble()

    fun checkoutIntent(m: Membership): CheckoutIntent {
        if (m.vip) {
            return CheckoutIntent.vip
        }

        if (m.autoRenewOffExpired) {
            return CheckoutIntent.newMember
        }

        return when (m.normalizedPayMethod) {
            PayMethod.ALIPAY,
            PayMethod.WXPAY -> CheckoutIntent(
                kind = IntentKind.OneTimeToAutoRenew,
                message = "使用Stripe转为自动续订，当前剩余时间将在新订阅失效后再次启用"
            )

            PayMethod.STRIPE -> if (m.tier == tier) {
                if (m.cycle == periodCount.toCycle()) {
                    CheckoutIntent(
                        kind = IntentKind.Forbidden,
                        message = "自动续订不能重复订阅"
                    )
                } else {
                    CheckoutIntent(
                        kind = IntentKind.SwitchInterval,
                        message = "更改Stripe自动扣款周期，建议您订阅年度版更划算"
                    )
                }
            } else {
                when (tier) {
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

            else -> CheckoutIntent.intentUnknown
        }
    }
}
