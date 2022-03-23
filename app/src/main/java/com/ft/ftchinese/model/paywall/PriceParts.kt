package com.ft.ftchinese.model.paywall

import java.util.*

data class PriceParts(
    val symbol: String,
    val amount: String,
    val cycle: String,
    val notes: String = "",
) {
    fun string(): String {
        return "${symbol}${amount}${cycle}"
    }

    companion object {
        val symbols = mapOf(
            "cny" to "¥",
            "usd" to "$",
            "gbp" to "£",
        )

        fun findSymbol(currency: String): String {
            return if (currency.isBlank()) {
                ""
            } else {
                symbols[currency] ?: currency.uppercase(Locale.ROOT)
            }
        }
    }
}
