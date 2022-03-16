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
    val introductory: Price? = null,
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

    fun introPrice(m: Membership): CartItemFtc? {
        return if ((introductory?.isValid() == true) && m.isZero) {
            CartItemFtc(
                price = introductory,
                discount = null,
                isIntro = true,
                stripeTrialParents = prices.map { it.stripePriceId }
            )
        } else null
    }

    /**
     * Compose information required by price buttons on ui.
     */
    fun recurringPrices(m: Membership): List<CartItemFtc> {

        val offerKinds = m.offerKinds

        return prices.map {
            CartItemFtc(
                price = it,
                discount = it.applicableOffer(offerKinds),
                isIntro = false,
                stripeTrialParents = listOf(),
            )
        }
    }
}
