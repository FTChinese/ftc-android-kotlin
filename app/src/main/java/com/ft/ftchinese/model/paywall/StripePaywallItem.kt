package com.ft.ftchinese.model.paywall

import com.ft.ftchinese.model.stripesubs.StripeCoupon
import com.ft.ftchinese.model.stripesubs.StripePrice
import kotlinx.serialization.Serializable

@Serializable
data class StripePaywallItem(
    val price: StripePrice,
    val coupons: List<StripeCoupon>,
) {
    fun applicableCoupon(): StripeCoupon? {
        if (coupons.isEmpty()) {
            return null
        }

        val filtered = coupons.filter { it.isValid() }.sortedByDescending { it.amountOff }

        if (filtered.isEmpty()) {
            return null
        }

        return filtered[0]
    }
}
