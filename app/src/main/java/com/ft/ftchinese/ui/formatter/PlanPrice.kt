package com.ft.ftchinese.ui.formatter

import android.content.Context
import com.ft.ftchinese.R
import com.ft.ftchinese.model.subscription.Plan
import com.ft.ftchinese.ui.formatter.formatPriceCycle

data class PlanPrice(
    val payable: String,         // The actually charged amount
    val original: String?, // The original price if discount exists.
)

fun buildPlanPrice(ctx: Context, plan: Plan): PlanPrice {
    val item = plan.checkoutItem()

    return PlanPrice(
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
