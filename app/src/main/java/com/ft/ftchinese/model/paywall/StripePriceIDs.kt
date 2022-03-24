package com.ft.ftchinese.model.paywall

import android.os.Parcelable
import com.ft.ftchinese.model.enums.OrderKind
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.stripesubs.StripePrice
import kotlinx.parcelize.Parcelize

/**
 * Used to pass as parcel.
 */
@Deprecated("")
@Parcelize
data class StripePriceIDs(
    val orderKind: OrderKind,
    val recurring: String,
    val trial: String?,
) : Parcelable

/**
 * Stripe price ids attached to a specific ftc product.
 */
data class StripePriceIDsOfProduct(
    val recurring: List<String>,
    val trial: String?,
) {
    fun listShoppingItems(prices: Map<String, StripePrice>, m: Membership): List<CartItemStripeV2> {
        if (prices.isEmpty()) {
            return listOf()
        }

        val trialPrice = trial?.let { prices[it] }

        val items = mutableListOf<CartItemStripeV2>()

        recurring.forEach { id ->
            val price = prices[id]
            if (price != null) {
                items.add(CartItemStripeV2(
                    intent = CheckoutIntent.ofStripe(m, price),
                    recurring = price,
                    trial = trialPrice
                ))
            }
        }

        return items
    }

    companion object {
        fun newInstance(ftcItems: List<CartItemFtcV2>): StripePriceIDsOfProduct {
            var trial: String? = null
            val recur = mutableListOf<String>()

            ftcItems.forEach {
                if (it.isIntro) {
                    trial = it.price.stripePriceId
                } else {
                    recur.add(it.price.stripePriceId)
                }
            }

            return StripePriceIDsOfProduct(
                recurring = recur,
                trial = trial,
            )
        }
    }
}
