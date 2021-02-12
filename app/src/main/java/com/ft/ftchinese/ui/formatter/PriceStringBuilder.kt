package com.ft.ftchinese.ui.formatter

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.subscription.Discount
import com.ft.ftchinese.model.subscription.Price

/**
 * Produces string like:
 * ¥1,998 by default
 * or ¥1,998/年 if cycle is provided
 */
class PriceStringBuilder(
    val currency: String,
    val amount: Double,
    val cycle: Cycle? = null
) {
    private var strikeThrough: Boolean = false
    private var scaleProportion: Float = 0f
    private var original: Boolean = false

    // Produces a string with effects like HTML <del> element
    fun withStrikeThrough(strike: Boolean = true): PriceStringBuilder {
        strikeThrough = strike
        return this
    }

    // Produces a string with ¥1,998 part scaled.
    fun withScale(proportion: Float = 2f): PriceStringBuilder {
        scaleProportion = proportion
        return this
    }

    // Produce a string like 原价 ¥1,998/年
    fun withOriginal(): PriceStringBuilder {
        this.original = true
        return this
    }

    fun build(ctx: Context): Spannable {
        var str = if (cycle == null) {
            ctx.getString(
                R.string.formatter_price,
                getCurrencySymbol(currency),
                amount
            )
        } else {
            ctx.getString(
                R.string.formatter_price_cycle,
                getCurrencySymbol(currency),
                amount,
                ctx.getString(cycle.stringRes),
            )
        }

        if (original) {
            str = ctx.getString(
                R.string.formatter_original_price,
                str
            )
        }

        return SpannableString(str).apply {
            if (strikeThrough) {
                setSpan(StrikethroughSpan(), 0, length - 1, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            }

            if (scaleProportion > 0) {
                val endIndex = if (cycle == null) {
                    length
                } else {
                    length - 2
                }

                setSpan(RelativeSizeSpan(scaleProportion), 1, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    companion object {
        @JvmStatic
        fun fromPrice(
            price: Price,
            withCycle: Boolean = true,
            discount: Discount? = null,
        ): PriceStringBuilder {
            return PriceStringBuilder(
                currency = price.currency,
                amount = if (discount == null) {
                    price.unitAmount
                } else {
                    price.unitAmount - (discount.priceOff ?: 0.0)
                },
                cycle = if (withCycle) price.cycle else null,
            )
        }
    }
}
