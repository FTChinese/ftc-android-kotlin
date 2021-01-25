package com.ft.ftchinese.ui.paywall

data class PromoUI(
    val heading: String,
    val subHeading: String?,
    val terms: String?
)

data class Price(
    val amount: String, // The actually charged amount
    val originalPrice: String?

)
