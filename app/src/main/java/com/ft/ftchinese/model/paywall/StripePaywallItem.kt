package com.ft.ftchinese.model.paywall

import com.ft.ftchinese.model.stripesubs.StripeCoupon
import com.ft.ftchinese.model.stripesubs.StripePrice
import kotlinx.serialization.Serializable

@Serializable
data class StripePaywallItem(
    val price: StripePrice,
    val coupons: List<StripeCoupon>,
) {
    fun getCoupon(): StripeCoupon? {
        return when (coupons.size) {
            0 -> null
            1 -> coupons[0]
            else ->  coupons.sortedByDescending { it.amountOff }[0]
        }
    }
}
