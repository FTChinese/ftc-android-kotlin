package com.ft.ftchinese.model.paywall

import org.threeten.bp.ZonedDateTime

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
}


