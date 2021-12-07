package com.ft.ftchinese.model.paywall

import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.fetch.KTier
import com.ft.ftchinese.model.ftcsubs.Price
import com.ft.ftchinese.model.reader.Membership

/**
 * Defines the data to present product on paywall.
 * By default the data is hard-coded in string resources.
 */
data class PaywallProduct(
    val id: String,
    @KTier
    val tier: Tier,
    val heading: String,
    val description: String,
    val smallPrint: String?,
    val introductory: Introductory,
    val prices: List<Price>,
) {
    fun descWithDailyCost(): String {
        if (description.isEmpty()) {
            return ""
        }

        var desc = description
        prices.map { it.dailyPrice() }
            .forEach {
                desc = desc.replace(it.holder, it.replacer)
            }

        return desc
    }

    /**
     * Compose information required by price buttons on ui.
     */
    fun checkoutPrices(m: Membership): List<CheckoutPrice> {
        return prices.map {
            CheckoutPrice(
                introductory = introductory,
                regular = UnifiedPrice.fromFtc(it, null),
                favour = it.applicableOffer(m.offerKinds)?.let { discount ->
                    UnifiedPrice.fromFtc(it, discount)
                }
            )
        }
    }
}
