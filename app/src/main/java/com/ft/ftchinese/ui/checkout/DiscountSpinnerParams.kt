package com.ft.ftchinese.ui.checkout

import com.ft.ftchinese.model.subscription.Discount

data class DiscountSpinnerParams(
    val items: List<Discount> = listOf(),
    val selectedIndex: Int = -1,
) {
    val hasDiscount: Boolean
        get() = items.isNotEmpty()
}

