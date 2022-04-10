package com.ft.ftchinese.model.paywall

import kotlinx.serialization.Serializable
import org.threeten.bp.ZonedDateTime

@Serializable
data class Paywall(
    val id: Int,
    val banner: Banner,
    val promo: Banner,
    val products: List<PaywallProduct>,
    val liveMode: Boolean = true,
) {
    fun isPromoValid(): Boolean {
        if (promo.id.isEmpty()) {
            return false
        }

        if (promo.startUtc == null || promo.endUtc == null) {
            return false
        }

        val now = ZonedDateTime.now()

        return !now.isBefore(promo.startUtc) && !now.isAfter(promo.endUtc)
    }

    fun reOrderProducts(premiumOnTop: Boolean): Paywall {
        return Paywall(
            id = id,
            banner = banner,
            promo = promo,
            products = if (premiumOnTop) {
                products.sortedByDescending { it.tier.ordinal }
            } else {
                products.sortedBy { it.tier.ordinal }
            },
            liveMode = liveMode,
        )
    }
}


