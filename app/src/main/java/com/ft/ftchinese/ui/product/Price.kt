package com.ft.ftchinese.ui.product

data class Price(
    val amount: String,         // The actually charged amount
    val originalPrice: String?, // The original price if discount exists.
)
