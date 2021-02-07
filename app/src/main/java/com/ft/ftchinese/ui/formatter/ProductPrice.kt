package com.ft.ftchinese.ui.formatter

import android.content.Context
import com.ft.ftchinese.R
import com.ft.ftchinese.model.subscription.Plan

data class ProductPrice(
    val payable: String,         // The actually charged amount
    val original: String?, // The original price if discount exists.
)

fun buildFtcPrice(ctx: Context, plan: Plan): ProductPrice {
    val item = plan.checkoutItem()

    return ProductPrice(
        payable = formatPriceCycle(
            ctx = ctx,
            price = item.originalPriceParams),

        original = if (item.discount != null) {
            ctx.getString(R.string.original_price) + formatPriceCycle(
                ctx = ctx,
                price = item.payablePriceParams)
        } else {
            null
        },
    )
}
