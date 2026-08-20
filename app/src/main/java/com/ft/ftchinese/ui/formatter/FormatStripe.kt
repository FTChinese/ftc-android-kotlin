package com.ft.ftchinese.ui.formatter

import android.content.Context
import com.ft.ftchinese.model.paywall.convertStripeAmount
import com.ft.ftchinese.model.paywall.getCurrencySymbol
import java.text.NumberFormat
import java.util.Locale

fun formatStripeAmount(ctx: Context, currency: String, amountMinor: Int): String {
    val amount = convertStripeAmount(currency, amountMinor)
    val formatter = NumberFormat.getNumberInstance(
        ctx.resources.configuration.locale ?: Locale.getDefault()
    ).apply {
        minimumFractionDigits = amount.scale()
        maximumFractionDigits = amount.scale()
    }
    return "${getCurrencySymbol(currency)}${formatter.format(amount)}"
}

fun formatStripeAmount(currency: String, amountMinor: Int): String {
    val amount = convertStripeAmount(currency, amountMinor)
        .stripTrailingZeros()
        .toPlainString()
    return "${getCurrencySymbol(currency)}$amount"
}
