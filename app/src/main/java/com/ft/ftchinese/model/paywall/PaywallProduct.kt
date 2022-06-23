package com.ft.ftchinese.model.paywall

import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.ftcsubs.Price
import com.ft.ftchinese.model.reader.Membership
import kotlinx.serialization.Serializable

/**
 * Defines the data to present product on paywall.
 * By default the data is hard-coded in string resources.
 */
@Serializable
data class PaywallProduct(
    val id: String,
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

    private fun buildContent(): ProductContent {
        return ProductContent(
            id = id,
            tier = tier,
            heading = heading,
            description = descWithDailyCost(),
            smallPrint = smallPrint,
        )
    }

    // Collect price items application to a membership.
    private fun collectFtcItems(m: Membership): Pair<List<CartItemFtc>, StripePriceIDs> {
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
            return Pair(
                recurringItems,
                StripePriceIDs(
                    recurring = prices.map { it.stripePriceId },
                    trial = null
                )
            )
        }

        return Pair(
            listOf(
                CartItemFtc(
                    intent = CheckoutIntent.newMember,
                    price = introductory,
                    discount = null,
                    isIntro = true,
                )
            ) + recurringItems,
            StripePriceIDs(
                recurring = prices.map { it.stripePriceId },
                trial = introductory.stripePriceId
            )
        )
    }

    fun buildUiItem(m: Membership, stripeStore: Map<String, StripePaywallItem>): ProductItem {
        val content = buildContent()

        val (ftcItems, stripeIds) = collectFtcItems(m)

        val stripeItems = stripeIds.buildCartItems(
            prices = stripeStore,
            m = m
        )

        return ProductItem(
            content = content,
            ftcItems = ftcItems,
            stripeItems = stripeItems,
        )
    }
}

data class ProductContent(
    val id: String,
    val tier: Tier,
    val heading: String,
    val description: String,
    val smallPrint: String?,
)

data class ProductItem(
    val content: ProductContent,
    val ftcItems: List<CartItemFtc>,
    val stripeItems: List<CartItemStripe>,
)
