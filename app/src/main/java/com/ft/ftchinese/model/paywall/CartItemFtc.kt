package com.ft.ftchinese.model.paywall

import android.os.Parcelable
import com.ft.ftchinese.model.ftcsubs.Discount
import com.ft.ftchinese.model.ftcsubs.Price
import com.ft.ftchinese.model.ftcsubs.YearMonthDay
import kotlinx.parcelize.Parcelize

@Parcelize
data class CartItemFtc(
    val price: Price,
    val discount: Discount?, // Introductory price does not have a discount.
    val isIntro: Boolean = false,
    val stripeTrialParents: List<String>,
): Parcelable {

    fun payableAmount(): Double {
        return price.unitAmount - (discount?.priceOff ?: 0.0)
    }

    fun stripeTrialId(): String? {
        if (!isIntro) {
            return null
        }

        return price.stripePriceId
    }
}

data class CartItemFtcV2(
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
