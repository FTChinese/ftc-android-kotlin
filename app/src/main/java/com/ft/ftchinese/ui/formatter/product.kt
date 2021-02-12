package com.ft.ftchinese.ui.formatter

import android.content.Context
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.Edition
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
 */
fun formatEdition(ctx: Context, e: Edition): String {
    return ctx.getString(
        R.string.formatter_edition,
        ctx.getString(e.tier.stringRes),
        ctx.getString(e.cycle.stringRes)
    )
}

