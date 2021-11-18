package com.ft.ftchinese.model.paywall

import android.os.Parcelable
import com.ft.ftchinese.model.ftcsubs.Price
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.stripesubs.StripePriceStore
import kotlinx.parcelize.Parcelize

@Parcelize
data class CheckoutPrice(
    // Always the price you should usually charge user.
    val regular: UnifiedPrice,
    // A discounted price for ftc, or introductory price for stripe.
    // For ftc, those two prices always share the same id.
    // For stripe, the price ids are different and you are
    // to provide both.
    val favour: UnifiedPrice?
): Parcelable {

    /**
     * Ensure live mode cannot be used by test user.
     */
    fun validateLiveMode(isTest: Boolean): Boolean {
        return regular.liveMode != isTest
    }

    companion object {
        @JvmStatic
        fun fromFtc(price: Price, m: Membership): CheckoutPrice {
            val discount = price.applicableOffer(m.offerKinds)

            return CheckoutPrice(
                regular = UnifiedPrice.fromFtc(price, null),
                // If there's discount available for this price and this user
                favour = discount?.let {
                    UnifiedPrice.fromFtc(price, it)
                },
            )
        }

        @JvmStatic
        fun fromStripe(priceId: String, introEligible: Boolean): CheckoutPrice? {
            val price = StripePriceStore.findById(priceId) ?: return null

            val intro = if (introEligible) {
                StripePriceStore.findIntroductory(price.product)
            } else {
                null
            }

            return CheckoutPrice(
                regular = UnifiedPrice.fromStripe(price),
                favour = intro?.let{
                    UnifiedPrice.fromStripe(it)
                },
            )
        }
    }
}
