package com.ft.ftchinese.ui.checkout

import com.ft.ftchinese.model.ftcsubs.Discount

data class DiscountOptions(
    val discounts: List<Discount> = listOf(),
) {

    private var selectedIndex: Int

    init {
        selectedIndex = findMaxDiscount()
    }

    val spinnerIndex: Int
        get() = selectedIndex

    val discountSelected: Discount?
        get() = if (selectedIndex > 0 && selectedIndex < discounts.size) {
            discounts[selectedIndex]
        } else null

    val hasDiscount: Boolean
        get() = discounts.isNotEmpty()

    fun changeDiscount(index: Int) {
        if (index > 0 && index < discounts.size) {
            selectedIndex = index
        }
    }
    // By default we pre-select the discount with highest rate.
    private fun findMaxDiscount(): Int {
        if (discounts.isEmpty()) {
            return -1
        }
        if (discounts.size == 1) {
            return 0
        }

        var maxIndex = 0
        for (i in 1 until discounts.size) {
            if (discounts[i].priceOff == null || discounts[maxIndex].priceOff == null) {
                continue
            }
            if (discounts[i].priceOff!! > discounts[maxIndex].priceOff!!) {
                maxIndex = i
            }
        }

        return maxIndex
    }
}

