package com.ft.ftchinese.ui.formatter

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import com.ft.ftchinese.R
import com.ft.ftchinese.model.subscription.CheckoutItem
import com.ft.ftchinese.model.ui.PriceParams

data class ProductPrice(
    val payable: String,         // The actually charged amount
    val original: Spannable?, // The original price if discount exists.
)

/**
 * Use spannable to style text.
 * See https://developer.android.com/reference/android/text/style/StrikethroughSpan
 */
fun buildFtcPrice(ctx: Context, item: CheckoutItem): ProductPrice {

    return ProductPrice(
        payable = formatPriceCycle(
            ctx = ctx,
            price = item.payablePriceParams),

        original = if (item.discount != null) {
            SpannableString(
                ctx.getString(R.string.original_price) + formatPriceCycle(
                ctx = ctx,
                price = item.originalPriceParams)
            )
                .apply {
                    setSpan(StrikethroughSpan(), 0, length-1, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                }
        } else {
            null
        },
    )
}

fun buildStripePrice(ctx: Context, params: PriceParams): ProductPrice {
    return ProductPrice(
        payable = formatPriceCycle(
            ctx = ctx,
            price = params
        ),
        original = null,
    )
}
