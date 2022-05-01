package com.ft.ftchinese.model.stripesubs

import android.os.Parcelable
import com.stripe.android.model.PaymentMethod
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable


@Parcelize
@Serializable
data class StripePaymentMethod(
    val id: String,
    val customerId: String,
    val card: StripePaymentCard,
) : Parcelable {
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

@Parcelize
@Serializable
data class StripePaymentCard(
    val brand: String,
    val country: String,
    val expMonth: Int,
    val expYear: Int,
    val last4: String,
) : Parcelable
