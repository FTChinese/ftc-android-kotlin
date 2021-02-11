package com.ft.ftchinese.ui.formatter

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import com.ft.ftchinese.R
import com.ft.ftchinese.model.subscription.Price

data class ProductPrice(
    val payable: String,         // The actually charged amount
    val original: Spannable?, // The original price if discount exists.
)

fun buildPrice(ctx: Context, price: Price): ProductPrice {
    return ProductPrice(
        payable = formatPriceCycle(
            ctx = ctx,
            price = price.payablePriceParams),

        original = if (price.promotionOffer.isValid()) {
            SpannableString(
                ctx.getString(R.string.original_price) + formatPriceCycle(
                    ctx = ctx,
                    price = price.originalPriceParams)
            )
                .apply {
                    setSpan(StrikethroughSpan(), 0, length-1, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                }
        } else {
            null
        },
    )
}
