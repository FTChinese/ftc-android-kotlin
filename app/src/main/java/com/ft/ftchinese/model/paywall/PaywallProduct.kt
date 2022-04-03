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

    fun listShoppingItems(m: Membership): List<CartItemFtc> {
        val offerKinds = m.offerKinds

        val recurringItems = prices.map { price ->
            CartItemFtc(
                intent = price.checkoutIntent(m),
                price = price,
                discount = price.filterOffer(offerKinds),
                isIntro = false,
            )
        }

        if (introductory == null || !introductory.isValid() || !m.isZero) {
            return recurringItems
        }

        return listOf(
            CartItemFtc(
                intent = CheckoutIntent.newMember,
                price = introductory,
                discount = null,
                isIntro = true,
            )
        ) + recurringItems
    }
}
