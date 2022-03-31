package com.ft.ftchinese.ui.formatter

import android.content.Context
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.*
import com.ft.ftchinese.model.ftcsubs.YearMonthDay
import com.ft.ftchinese.model.paywall.CartItemFtc
import com.ft.ftchinese.model.paywall.IntentKind
import com.ft.ftchinese.model.paywall.PaymentIntent
import com.ft.ftchinese.model.stripesubs.StripePrice
import java.util.*

object FormatHelper {
    fun currencySymbol(currency: String): String {
        if (currency.isEmpty()) {
            return ""
        }

        return when (currency) {
            "cny" -> "¥"
            "usd" -> "$"
            "gbp" -> "£"
            else -> currency.uppercase(Locale.ROOT)
        }
    }

    fun getTier(ctx: Context, tier: Tier): String {
        return when (tier) {
            Tier.STANDARD -> ctx.getString(R.string.tier_standard)
            Tier.PREMIUM -> ctx.getString(R.string.tier_premium)
        }
    }

    private fun getCycle(ctx: Context, cycle: Cycle): String {
        return when (cycle) {
            Cycle.MONTH -> ctx.getString(R.string.cycle_month)
            Cycle.YEAR -> ctx.getString(R.string.cycle_year)
        }
    }

    fun cycleOfYMD(ctx: Context, ymd: YearMonthDay): String {
        return getCycle(ctx, ymd.toCycle())
    }

    fun getCycleN(ctx: Context, cycle: Cycle, n: Int): String {
        return "${n}${getCycle(ctx, cycle)}"
    }

    fun getPayMethod(ctx: Context, pm: PayMethod): String {
        return when (pm) {
            PayMethod.ALIPAY -> ctx.getString(R.string.pay_method_ali)
            PayMethod.WXPAY -> ctx.getString(R.string.pay_method_wechat)
            PayMethod.STRIPE -> ctx.getString(R.string.pay_method_stripe)
            PayMethod.APPLE -> ctx.getString(R.string.pay_brand_apple)
            PayMethod.B2B -> ctx.getString(R.string.pay_brand_b2b)
        }
    }

    private fun getOrderKind(ctx: Context, k: OrderKind): String {
        return when (k) {
            OrderKind.Create -> ctx.getString(R.string.subs_create)
            OrderKind.Renew -> ctx.getString(R.string.subs_renew)
            OrderKind.Upgrade -> ctx.getString(R.string.subs_upgrade)
            OrderKind.Downgrade -> "降级"
            OrderKind.AddOn -> ctx.getString(R.string.subs_addon)
            OrderKind.SwitchCycle -> ctx.getString(R.string.stripe_switch_cycle)
        }
    }

    /**
     * Generate human readable string like:
     * 标准会员/年
     * 标准会员/月
     * 高级会员/年
     */
    fun formatEdition(ctx: Context, e: Edition): String {
        return ctx.getString(
            R.string.formatter_edition,
            getTier(ctx, e.tier),
            getCycle(ctx, e.cycle),
        )
    }

    fun formatEdition(ctx: Context, tier: Tier, ymd: YearMonthDay): String {
        val t = getTier(ctx, tier)
        val c = getCycle(ctx, ymd.toCycle())

        return "$t/$c"
    }

    /**
     * Format money into a string with 2 precision: 298.00
     */
    fun formatMoney(ctx: Context, amount: Double): String {
        return ctx.getString(
            R.string.formatter_money,
            amount
        )
    }

    @Deprecated("")
    fun formatPrice(ctx: Context, currency: String, amount: Double): String {
        return "${currencySymbol(currency)}${formatMoney(ctx, amount)}"
    }

    /**
     * Format year month day to a string like:
     * - 1年3个月14天
     * - 1年3个月
     * - 1年14天
     * - 3个月14天
     * The year, month, day should have at least 2 fields larger than 0.
     */
    @Deprecated("")
    private fun formatYMD(ctx: Context, ymd: YearMonthDay): String {
        val sb = StringBuilder()

        if (ymd.years > 0) {
            sb.append(ctx.getString(R.string.count_years, ymd.years))
        }

        if (ymd.months > 0) {
            sb.append(ctx.getString(R.string.count_months, ymd.months))
        }

        if (ymd.days > 0) {
            sb.append(ctx.getString(R.string.count_days, ymd.days))
        }

        return sb.toString()
    }

    /**
     * Format trials period based on year, month day field.
     * When there is only one of the year, month, day is
     * larger than 0, the string should be like:
     * - 首年
     * - 首月
     * - 前xx年
     * - 前xx月
     * - 前xx天
     * When there are more than one fields larger than 0,
     * it will use formatYMD() method.
     */
    fun formatTrialPeriod(ctx: Context, ymd: YearMonthDay): String {
        if (ymd.isYearOnly()) {
            return if (ymd.years > 1) {
                "${ctx.getString(R.string.prefix_first)}${ctx.getString(R.string.count_years, ymd.years)}"
            } else {
                ctx.getString(R.string.initial_year)
            }
        }

        if (ymd.isMonthOnly()) {
            return if (ymd.months > 1) {
                "${ctx.getString(R.string.prefix_first)}${ctx.getString(R.string.count_months, ymd.months)}"
            } else {
                ctx.getString(R.string.intial_month)
            }
        }

        if (ymd.isDayOnly()) {
            return "${ctx.getString(R.string.prefix_first)}${ctx.getString(R.string.count_days, ymd.days)}"
        }

        return formatYMD(ctx, ymd)
    }

    /**
     * Format trials period based on year, month day field.
     * When there is only one of the year, month, day is
     * larger than 0, the string should be like:
     * - 年
     * - 月
     * - xx年
     * - xx月
     * - xx天
     * When there are more than one fields larger than 0,
     * it will use formatYMD() method.
     */
    fun formatRegularPeriod(ctx: Context, ymd: YearMonthDay): String {
        if (ymd.isYearOnly()) {
            return if (ymd.years > 1) {
                // Example: 2年
                ctx.getString(R.string.count_years, ymd.years)
            } else {
                // 年
                ctx.getString(R.string.cycle_year)
            }
        }

        if (ymd.isMonthOnly()) {
            return if (ymd.months > 1) {
                // Example: 3个月
                ctx.getString(R.string.count_months, ymd.months)
            } else {
                // 月
                ctx.getString(R.string.cycle_month)
            }
        }

        // Example: 14天
        if (ymd.isDayOnly()) {
            return ctx.getString(R.string.count_days, ymd.days)
        }

        return formatYMD(ctx, ymd)
    }

    private fun formatCheckoutPrice(ctx: Context, item: CartItemFtc): String {
        return formatPrice(
            ctx,
            item.price.currency,
            item.payableAmount()
        )
    }

    fun payButton(ctx: Context, intent: PaymentIntent): String {
        return when (intent.payMethod) {
            PayMethod.ALIPAY, PayMethod.WXPAY -> {
                // Ali/Wx pay button have two groups:
                // CREATE/RENEW/UPGRADE: 支付宝支付 ¥258.00 or 微信支付 ¥258.00
                // ADD_ON: 购买订阅期限
                if (intent.orderKind == OrderKind.AddOn) {
                    getOrderKind(ctx, intent.orderKind)
                } else {
                    "${getPayMethod(ctx, intent.payMethod)} ${formatCheckoutPrice(ctx, intent.item)}"
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
                when (intent.orderKind) {
                    OrderKind.Create -> getPayMethod(ctx,intent.payMethod)
                    // Renew is used by alipay/wxpay switching to Stripe.
                    OrderKind.Renew -> ctx.getString(R.string.switch_to_stripe)
                    // This might be stripe standard upgrade, or ali/wx standard switching payment method.
                    OrderKind.Upgrade -> getPayMethod(
                        ctx,
                        intent.payMethod) + getTier(
                        ctx,
                        intent.item.price.tier)
                    OrderKind.SwitchCycle -> ctx.getString(R.string.pay_brand_stripe) + getOrderKind(
                        ctx,
                        intent.orderKind
                    )
                    OrderKind.AddOn -> "Stripe订阅不支持一次性购买"
                    OrderKind.Downgrade -> "Stripe订阅不支持降级"
                }
            }
            PayMethod.APPLE -> "无法处理苹果订阅"
            PayMethod.B2B -> "暂不支持企业订阅"
        }
    }

    fun stripePricePeriod(ctx: Context, price: StripePrice, isTrial: Boolean = false): String {
        val period = if (isTrial) {
            formatTrialPeriod(ctx, price.periodCount)
        } else {
            formatRegularPeriod(ctx, price.periodCount)
        }

        val priceStr = formatPrice(ctx, price.currency, price.moneyAmount)

        return "$priceStr/$period"
    }

    fun stripeTrialMessage(ctx: Context, price: StripePrice): String {

        val pricePeriod = stripePricePeriod(ctx, price, true)

        return ctx.getString(R.string.stripe_trial_message, pricePeriod)
    }

    fun stripeAutoRenewalMessage(ctx: Context, price: StripePrice, isTrial: Boolean): String {
        val pricePeriod = stripePricePeriod(ctx, price)

        return if (isTrial) {
            ctx.getString(R.string.after_trial_ends)
        } else {
            ""
        } + ctx.getString(R.string.auto_renewal_message, pricePeriod)
    }

    fun stripeIntentText(ctx: Context, kind: IntentKind): String {
        return when (kind) {
            IntentKind.SwitchInterval -> ctx.getString(R.string.stripe_switch_cycle)
            IntentKind.Upgrade -> ctx.getString(R.string.stripe_upgrade)
            IntentKind.Downgrade -> ctx.getString(R.string.stripe_downgrade)
            else -> ctx.getString(R.string.subs_create)
        }
    }
}
