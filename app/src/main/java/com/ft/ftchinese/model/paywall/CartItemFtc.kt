package com.ft.ftchinese.model.paywall

import com.ft.ftchinese.model.ftcsubs.Discount
import com.ft.ftchinese.model.ftcsubs.Price
import com.ft.ftchinese.model.ftcsubs.YearMonthDay

data class CartItemFtc(
    val intent: CheckoutIntent,
    val price: Price,
    val discount:  Discount? = null,
    val isIntro: Boolean
) {
    fun normalizePeriod(): YearMonthDay {
        if (discount != null && !discount.overridePeriod.isZero()) {
            return discount.overridePeriod
        }

        return price.periodCount
    }

    fun payableAmount(): Double {
        return price.unitAmount - (discount?.priceOff ?: 0.0)
    }
}
