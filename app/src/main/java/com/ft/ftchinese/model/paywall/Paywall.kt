package com.ft.ftchinese.model.paywall

import com.ft.ftchinese.model.enums.Edition
import com.ft.ftchinese.model.ftcsubs.Price
import org.threeten.bp.ZonedDateTime

data class Paywall(
    val id: Int,
    val banner: Banner,
    val promo: Banner,
    val products: List<PaywallProduct>,
    val liveMode: Boolean = true,
) {

    fun findPrice(e: Edition): Price? {
        return products
            .find { it.tier == e.tier }
            ?.prices
            ?.find { it.cycle == e.cycle }
    }

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


