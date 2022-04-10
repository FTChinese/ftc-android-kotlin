package com.ft.ftchinese.model.stripesubs

import com.stripe.android.model.PaymentMethod
import kotlinx.serialization.Serializable

@Serializable
data class StripePaymentMethod(
    val id: String,
    val customerId: String,
    val card: StripePaymentCard,
) {
    companion object {
        @JvmStatic
        fun newInstance(pm: PaymentMethod): StripePaymentMethod {
            return StripePaymentMethod(
                id = pm.id ?: "",
                customerId = pm.customerId ?: "",
                card = StripePaymentCard(
                    brand = pm.card?.brand?.displayName ?: "",
                    country = pm.card?.country ?: "",
                    expMonth = pm.card?.expiryMonth ?: 0,
                    expYear = pm.card?.expiryYear ?: 0,
                    last4 = pm.card?.last4 ?: "",
                )
            )
        }
    }
}

@Serializable
data class StripePaymentCard(
    val brand: String,
    val country: String,
    val expMonth: Int,
    val expYear: Int,
    val last4: String,
)
