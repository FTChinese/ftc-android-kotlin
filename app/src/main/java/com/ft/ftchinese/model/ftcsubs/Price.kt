package com.ft.ftchinese.model.ftcsubs

import android.os.Parcelable
import com.ft.ftchinese.model.enums.*
import com.ft.ftchinese.model.paywall.CartItemFtc
import com.ft.ftchinese.model.paywall.CheckoutIntent
import com.ft.ftchinese.model.paywall.IntentKind
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.serializer.DateTimeAsStringSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.threeten.bp.ZonedDateTime

/**
 * Price contains data of a product's price.
 * It unifies both ftc and Stripe product.
 */
@Parcelize
@Serializable
data class Price(
    val id: String,
    val tier: Tier,
    val cycle: Cycle?,
    val active: Boolean = true,
    val currency: String = "cny",
    val kind: PriceKind? = null,
    val liveMode: Boolean,
    val nickname: String? = null,
    val periodCount: YearMonthDay = YearMonthDay(),
    val productId: String,
    val stripePriceId: String,
    val title: String? = null,
    val unitAmount: Double,
    @Serializable(with = DateTimeAsStringSerializer::class)
    val startUtc: ZonedDateTime? = null,
    @Serializable(with = DateTimeAsStringSerializer::class)
    val endUtc: ZonedDateTime? = null,
    val offers: List<Discount> = listOf(), // Does not exist when used as introductory price.
) : Parcelable {

    fun toJsonString(): String {
        return Json.encodeToString(this)
    }

    private val isIntro: Boolean
        get() = kind == PriceKind.OneTime

    val edition: Edition
        get() = Edition(
            tier = tier,
            cycle = cycle ?: periodCount.toCycle()
        )

    fun normalizeYMD(): YearMonthDay {
        if (!periodCount.isZero()) {
            return periodCount
        }

        return cycle?.let {
            YearMonthDay.of(it)
        }
            ?: YearMonthDay.zero()
    }

    fun isValid(): Boolean {
        if (startUtc == null || endUtc == null) {
            return true
        }

        val now = ZonedDateTime.now()

        if (now.isBefore(startUtc) || now.isAfter(endUtc)) {
            return false
        }

        return true
    }

    fun dailyPrice(): DailyPrice {
        val days = if (periodCount.isZero()) {
            1
        } else {
            periodCount.totalDays()
        }

        val avg = unitAmount.div(days)

        return when (periodCount.toCycle()) {
            Cycle.YEAR -> DailyPrice.ofYear(avg)
            Cycle.MONTH -> DailyPrice.ofMonth(avg)
        }
    }

    /**
     * This method might be relocated to ui package
     * if we want to move hard-coded message to string resources.
     */
    fun checkoutIntent(m: Membership): CheckoutIntent {
        if (m.vip) {
            return CheckoutIntent.vip
        }

        if (m.autoRenewOffExpired) {
            return CheckoutIntent.newMember
        }

        return when (m.normalizedPayMethod) {
            PayMethod.ALIPAY, PayMethod.WXPAY -> if (m.tier == tier) {
                CheckoutIntent.oneTimeRenewal(m)
            } else {
                CheckoutIntent.oneTimeDifferTier(target = tier)
            }

            PayMethod.STRIPE -> if (m.tier == tier) {
                CheckoutIntent.autoRenewAddOn
            } else {
                when (tier) {
                    // Standard -> onetime premium
                    Tier.PREMIUM -> CheckoutIntent(
                        kind = IntentKind.Forbidden,
                        message = "Stripe标准版自动续订使用支付宝/微信购买的订阅时间只能在自动续订结束后才能升级，如果您希望升级到高端版，请继续使用Stripe支付升级"
                    )
                    Tier.STANDARD -> CheckoutIntent.autoRenewAddOn
                }
            }

            PayMethod.APPLE -> if (m.tier == Tier.STANDARD && tier == Tier.PREMIUM) {
                CheckoutIntent(
                    kind = IntentKind.Forbidden,
                    message = "当前标准会员会员来自苹果内购，升级高端会员需要在您的苹果设备上，使用原有苹果账号登录后，在FT中文网APP内操作"
                )
            } else {
                CheckoutIntent.autoRenewAddOn
            }

            PayMethod.B2B -> if (m.tier == Tier.STANDARD && tier == Tier.PREMIUM) {
                CheckoutIntent(
                    kind = IntentKind.Forbidden,
                    message = "当前订阅来自企业版授权，升级高端订阅请联系您所属机构的管理人员"
                )
            } else {
                CheckoutIntent.b2bAddOn
            }

            else -> CheckoutIntent.intentUnknown
        }
    }

    /**
     * Find out the most applicable discount a membership
     * could enjoy among this price's offers.
     */
    fun filterOffer(by: List<OfferKind>): Discount? {
        if (offers.isEmpty()) {
            return null
        }

        // Filter the offers that are in the filters
        // list. If there are multiple ones, use
        // the one with the biggest price off.
        // Ignore introductory discount which is no longer used.
        val filtered = offers.filter {
                it.kind != OfferKind.Introductory && it.isValid() && by.contains(it.kind)
            }
            .sortedByDescending {
                it.priceOff
            }

        if (filtered.isEmpty()) {
            return null
        }

        return filtered[0]
    }

    fun buildCartItem(m: Membership): CartItemFtc {
        val offerFilter = m.offerKinds

        return CartItemFtc(
            intent = checkoutIntent(m),
            price = this,
            discount = filterOffer(offerFilter),
            isIntro = isIntro && isValid()
        )
    }
}
