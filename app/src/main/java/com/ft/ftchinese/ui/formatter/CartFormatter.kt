package com.ft.ftchinese.ui.formatter

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.PriceSource
import com.ft.ftchinese.model.paywall.UnifiedPrice

/**
 * Format the price string put into a cart.
 */
data class CartFormatter(
    val currency: String,
    val amount: Double,
    val cycle: Cycle? = null,
    val trialDays: Int? = null,
) {
    private var amountScaleProportion: Float = 0f
    private var strikeThrough: Boolean = false
    private var prefix: String = ""

    /**
     * Build a string like：
     * - /年
     * - /月
     * - /首周
     * - /首月
     * - /首年
     * - /前2周
     * - /前3周
     * - /前2个月
     * - /前3个月
     * - /前100天
     * or empty string if neither cycle nor periodDays set.
     */
    private fun formatPeriod(ctx: Context): String {
        return when {
            cycle != null -> FormatHelper.getCycle(ctx, cycle)
            trialDays != null -> FormatHelper.trialPeriod(ctx, trialDays)
            else -> null
        }?.let {
            "/$it"
        } ?: ""
    }

    fun withPrefix(prefix: String): CartFormatter {
        this.prefix = prefix
        return this
    }

    /**
     * Cross over the original price if there's a discount.
     */
    fun withStrikeThrough(strike: Boolean = true): CartFormatter {
        strikeThrough = strike
        return this
    }

    /**
     * Scale the price part. Keep currency untouched.
     */
    fun withScale(proportion: Float = 2f): CartFormatter {
        amountScaleProportion = proportion
        return this
    }

    fun format(ctx: Context): Spannable {
        val currSymbol = FormatHelper.currencySymbol(currency)
        val amountStr = FormatHelper.formatMoney(ctx, amount)
        val period = formatPeriod(ctx)

        val str = "${prefix}$currSymbol${amountStr}${period}"

        return SpannableString(str).apply {
            // Cross over the currency symbol and price amount.
            if (strikeThrough) {
                setSpan(
                    StrikethroughSpan(),
                    prefix.length,
                    length - period.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }

            // Scale the price amount
            if (amountScaleProportion > 0) {
                setSpan(
                    RelativeSizeSpan(amountScaleProportion),
                    prefix.length + currSymbol.length,
                    length - period.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
        }
    }

    companion object {

        fun newInstance(price: UnifiedPrice): CartFormatter {
            when (price.source) {
                PriceSource.Ftc -> {
                    return CartFormatter(
                        currency = price.currency,
                        amount = price.unitAmount,
                        cycle =  price.cycle
                    )
                }
                PriceSource.Stripe -> {
                    return if (price.isIntroductory) {
                        CartFormatter(
                            currency = price.currency,
                            amount = price.unitAmount,
                            trialDays = price.periodDays
                        )
                    } else {
                        CartFormatter(
                            currency = price.currency,
                            amount = price.unitAmount,
                            cycle =  price.cycle
                        )
                    }
                }
            }

        }
    }
}
