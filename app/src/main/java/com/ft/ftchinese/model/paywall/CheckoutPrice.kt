package com.ft.ftchinese.model.paywall

import android.os.Parcelable
import com.ft.ftchinese.model.stripesubs.StripePriceStore
import kotlinx.parcelize.Parcelize

@Parcelize
data class CheckoutPrice(
    val introductory: Introductory, // Use this to find stripe introductory offer. Only exists when FTC product has it set and has an introductory kind discount.
    // Always the price you should usually charge user.
    val regular: UnifiedPrice,
    // A discounted price for ftc, or introductory price for stripe.
    // For ftc, those two prices always share the same id.
    // For stripe, the price ids are different and you are
    // to provide both.
    val favour: UnifiedPrice?
): Parcelable {

    /**
     * Compose stripe checkout price based on ftc price setting.
     */
    fun ofStripe(): CheckoutPrice? {
        val price = StripePriceStore
            .find(regular.stripePriceId)
            ?: return null

        val intro = if (introductory.stripePriceId.isNullOrEmpty()) {
            null
        } else {
            StripePriceStore.find(introductory.stripePriceId)
        }

        return CheckoutPrice(
            introductory = introductory,
            regular = UnifiedPrice.fromStripe(price),
            favour = intro?.let{
                UnifiedPrice.fromStripe(it)
            },
        )
    }
}
