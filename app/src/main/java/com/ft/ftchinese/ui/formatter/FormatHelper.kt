package com.ft.ftchinese.ui.formatter

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.paywall.CheckoutPrice
import com.ft.ftchinese.model.enums.Edition
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.paywall.UnifiedPrice
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

    fun getCycle(ctx: Context, cycle: Cycle): String {
        return when (cycle) {
            Cycle.MONTH -> ctx.getString(R.string.cycle_month)
            Cycle.YEAR -> ctx.getString(R.string.cycle_year)
        }
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

    /**
     * Format money into a string with 2 precision: 298.00
     */
    fun formatMoney(ctx: Context, amount: Double): String {
        return ctx.getString(
            R.string.formatter_money,
            amount
        )
    }

    fun formatPrice(ctx: Context, currency: String, amount: Double): String {
        return "${currencySymbol(currency)}${formatMoney(ctx, amount)}"
    }

    private fun formatPrice(ctx: Context, price: UnifiedPrice): String {
        return "${currencySymbol(price.currency)}${formatMoney(ctx, price.unitAmount)}"
    }

    /**
     * Turn a number of days of trial period into human readable string.
     * Some numbers have special meaning:
     * 7 - 首周
     * 14 - 前2周
     * 21 - 前3周
     * 30, 31 - 首月
     * 60, 61, 62 - 前2个月
     * 90, 91, 92, 93 - 前3个月
     * 365, 366 - 第1年
     * other numbers are simply converted to 前xxx天
     */
    fun trialPeriod(ctx: Context, days: Int): String {
        return when (days) {
            in 1..6 -> ctx.getString(R.string.initial_days, days)
            7 -> ctx.getString(R.string.first_week)
            14 -> ctx.getString(R.string.initial_weeks, 2)
            21 -> ctx.getString(R.string.initial_weeks, 3)
            in 30..31 -> ctx.getString(R.string.first_month)
            in 60..62 -> ctx.getString(R.string.initial_months, 2)
            in 90..93 -> ctx.getString(R.string.initial_months, 3)
            in 365..366 -> ctx.getString(R.string.first_year)
            else -> ctx.getString(R.string.initial_days, days)
        }
    }

    /**
     * The text shown on paywall.
     */
    fun priceButton(ctx: Context, cop: CheckoutPrice): Spannable {
        val regularPrice = formatPrice(ctx, cop.regular.currency, cop.regular.unitAmount)
        val period = "/${getCycle(ctx, cop.regular.cycle)}"

        // If no discount, use regular price.
        if (cop.favour == null) {
            return SpannableString("$regularPrice$period")
        }

        // If there's discount, regular price is crossed.
        val discountedPrice = formatPrice(ctx, cop.favour.currency, cop.favour.unitAmount)

        return SpannableString("$regularPrice $discountedPrice$period").apply {
            setSpan(StrikethroughSpan(), 0, regularPrice.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        }
    }

    fun payButton(ctx: Context, pm: PayMethod, price: UnifiedPrice): String {
        val pmStr = getPayMethod(ctx, pm)
        val priceStr = formatPrice(ctx, price)

        return "$pmStr $priceStr"
    }

    fun stripeTrialMessage(ctx: Context, price: UnifiedPrice): String {
        if (price.periodDays <= 0) {
            return ""
        }

        val period = trialPeriod(ctx, price.periodDays)
        val priceStr = formatPrice(ctx, price)

        return ctx.getString(R.string.stripe_trial_message, period, priceStr)
    }
}
