package com.ft.ftchinese.ui.product

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import com.ft.ftchinese.R
import com.ft.ftchinese.model.ftcsubs.CheckoutItem
import com.ft.ftchinese.ui.formatter.getCurrencySymbol

/**
 * Generate the price text. If the price has discount,
 * the original price will be crossed.
 */
fun formatPriceButton(ctx: Context, item: CheckoutItem): Spannable {
    val payableStr = ctx.getString(
        R.string.formatter_price_cycle,
        getCurrencySymbol(item.price.currency),
        item.payableAmount,
        ctx.getString(item.price.cycle.stringRes)
    )

    // Show original price.
    if (item.discount == null) {
        return SpannableString(payableStr)
    }

    val crossed = ctx.getString(
        R.string.formatter_price,
        getCurrencySymbol(item.price.currency),
        item.price.unitAmount
    )

    return SpannableString("$crossed $payableStr").apply {
        setSpan(StrikethroughSpan(), 0, crossed.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
    }
}
