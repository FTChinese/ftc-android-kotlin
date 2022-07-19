package com.ft.ftchinese.ui.formatter

import android.content.Context
import com.ft.ftchinese.R
import com.ft.ftchinese.model.paywall.MoneyParts
import com.ft.ftchinese.model.paywall.PriceParts

fun joinPriceParts(ctx: Context, parts: PriceParts): String {
    return "${parts.symbol}${formatMoney(ctx, parts.amount)}${parts.separator}${formatYMD(ctx, parts.period, parts.isRecurring)}"
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

fun formatMoneyParts(ctx: Context, parts: MoneyParts): String {
    return "${parts.symbol}${formatMoney(ctx, parts.amount)}"
}

fun formatAmountOff(ctx: Context, parts: MoneyParts): String {
    return "-${parts.symbol}${formatMoney(ctx, parts.amount)}"
}
