package com.ft.ftchinese.model.paywall

import com.ft.ftchinese.model.ftcsubs.YearMonthDay
import java.util.*
import java.math.BigDecimal
import java.math.RoundingMode

private val symbols = mapOf(
    "cny" to "¥",
    "aud" to "A$",
    "cad" to "C$",
    "eur" to "€",
    "usd" to "$",
    "gbp" to "£",
    "hkd" to "HK$",
    "jpy" to "¥",
    "mop" to "MOP$",
    "nzd" to "NZ$",
    "sgd" to "S$",
    "twd" to "NT$",
)

fun getCurrencySymbol(currency: String): String {
    return if (currency.isBlank()) {
        ""
    } else {
        symbols[currency] ?: currency.uppercase(Locale.ROOT)
    }
}

fun convertCent(amount: Int): Double {
    return amount
        .toBigDecimal()
        .divide(100.toBigDecimal())
        .toDouble()
}

/** Convert a Stripe amount in the currency's smallest unit to display units. */
fun convertStripeAmount(currency: String, amountMinor: Int): BigDecimal {
    val fractionDigits = runCatching {
        Currency.getInstance(currency.uppercase(Locale.ROOT)).defaultFractionDigits
    }.getOrDefault(2).coerceAtLeast(0)

    return BigDecimal(amountMinor).movePointLeft(fractionDigits)
        .setScale(fractionDigits, RoundingMode.UNNECESSARY)
}

data class MoneyParts(
    val symbol: String,
    val amount: Double,
)

data class PriceParts(
    val symbol: String,
    val amount: Double,
    val separator: String = "/",
    val period: YearMonthDay,
    val isRecurring: Boolean,
    val highlighted: Boolean = false,
    val crossed: Boolean = false,
)
