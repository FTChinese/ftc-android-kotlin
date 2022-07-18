package com.ft.ftchinese.model.paywall

import com.ft.ftchinese.model.ftcsubs.YearMonthDay
import java.util.*

private val symbols = mapOf(
    "cny" to "¥",
    "usd" to "$",
    "gbp" to "£",
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
