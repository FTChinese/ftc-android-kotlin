package com.ft.ftchinese.ui.paywall

data class UIProduct(
        val heading: String = "",
        val description: String = "",
        val smallPrint: String? = null
)

data class PromoUI(
    val heading: String,
    val subHeading: String?,
    val terms: String?
)

data class Price(
    val amount: String, // The actually charged amount
    val originalPrice: String?

)
