package com.ft.ftchinese.ui.formatter

import android.content.Context
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.ui.Price
import java.util.*

fun getCurrencySymbol(currency: String): String {
    return when (currency) {
        "cny" -> "¥"
        "usd" -> "$"
        "gbp" -> "£"
        else -> currency.toUpperCase(Locale.ROOT)
    }
}

/**
 * Generate human readable membership type.
 * 标准会员/年
 * 标准会员/月
 * 高级会员/年
 * deprecated
 */
@Deprecated("Cycle always comes with price")
fun formatTierCycle(ctx: Context, tier: Tier?, cycle: Cycle?): String {
    if (tier == null || cycle == null) {
        return ""
    }

    return ctx.getString(
        R.string.formatter_tier_cycle,
        ctx.getString(tier.stringRes),
        ctx.getString(cycle.stringRes)
    )
}

fun formatPrice(ctx: Context, price: Price): String {
    return ctx.getString(
        R.string.formatter_price,
        getCurrencySymbol(price.currency),
        price.amount
    )
}

/**
 * Produce a string like ¥1,998/年
 */
fun formatPriceCycle(ctx: Context, price: Price): String {
    return ctx.getString(
        R.string.formatter_price_cycle,
        getCurrencySymbol(price.currency),
        price.amount,
        ctx.getString(price.cycle.stringRes),
    )
}
