package com.ft.ftchinese.model.paywall

import android.os.Parcelable
import com.ft.ftchinese.model.ftcsubs.Discount
import com.ft.ftchinese.model.ftcsubs.Price
import kotlinx.parcelize.Parcelize

@Parcelize
data class FtcCheckout(
    val price: Price,
    val discount: Discount?,
    val isIntroductory: Boolean = false,
    val stripeTrialParents: List<String>,
): Parcelable {

    fun payableAmount(): Double {
        return price.unitAmount - (discount?.priceOff ?: 0.0)
    }

    fun stripeTrialId(): String? {
        if (!isIntroductory) {
            return null
        }

        return price.stripePriceId
    }
}
