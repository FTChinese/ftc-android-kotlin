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
    private fun descWithDailyCost(): String {
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

    fun buildContent(): ProductContent {
        return ProductContent(
            id = id,
            tier = tier,
            heading = heading,
            description = descWithDailyCost(),
            smallPrint = smallPrint,
        )
    }

    // Collect price items application to a membership.
    fun collectPriceItems(m: Membership): Pair<List<CartItemFtc>, StripePriceIDs> {
        val offerKinds = m.offerKinds

        val recurringItems = prices.map { price ->
            CartItemFtc(
                intent = CheckoutIntent.ofFtc(m, price),
                price = price,
                discount = price.filterOffer(offerKinds),
                isIntro = false,
            )
        }

        val introItem = if (m.isZero) {
            CartItemFtc.ofIntro(introductory)
        } else {
            null
        }

        if (introItem == null) {
            return Pair(
                recurringItems,
                StripePriceIDs(
                    recurring = prices.map { it.stripePriceId },
                    trial = null
                )
            )
        }

        return Pair(
            listOf(introItem) + recurringItems,
            StripePriceIDs(
                recurring = prices.map { it.stripePriceId },
                trial = introItem.price.stripePriceId
            )
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
) {
    companion object {
        @JvmStatic
        fun newInstance(
            product: PaywallProduct,
            m: Membership,
            stripeStore: Map<String, StripePaywallItem>
        ): ProductItem {
            val content = product.buildContent()

            val (ftcItems, stripeIds) = product.collectPriceItems(m)

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
}
