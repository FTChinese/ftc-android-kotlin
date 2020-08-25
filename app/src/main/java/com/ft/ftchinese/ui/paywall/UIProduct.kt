package com.ft.ftchinese.ui.paywall

data class UIProduct(
        val heading: String = "",
        val description: String = "",
        val smallPrint: String? = null
)

data class Price(
    val amount: String, // The actually charged amount
    val discountPeriod: String?, // The original price and discounted discount ending date.
    val originalPrice: String

)
