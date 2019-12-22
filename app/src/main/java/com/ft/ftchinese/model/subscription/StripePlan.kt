package com.ft.ftchinese.model.subscription

data class StripePlan(
        val id: String,
        val active: Boolean = true,
        val amount: Int,
        val currency: String,
        val interval: String
) {
    fun currencySymbol(): String {
        return when (currency) {
            "cny" -> "¥"
            "usd" -> "$"
            "gbp" -> "£"
            else -> "¥"
        }
    }

    fun price(): Double {
        return (amount / 100).toDouble()
    }
}
