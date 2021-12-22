package com.ft.ftchinese.model.paywall

import android.os.Parcelable
import com.ft.ftchinese.model.enums.OrderKind
import kotlinx.parcelize.Parcelize

@Parcelize
data class StripeCheckout(
    val orderKind: OrderKind,
    val recurringPriceId: String,
    val trialPriceId: String?,
) : Parcelable {

    fun counter(): StripeCounter? {
        return StripePriceStore.find(recurringPriceId)?.let { price ->
            StripeCounter(
                orderKind = orderKind,
                recurringPrice = price,
                trialPrice = trialPriceId?.let {
                    StripePriceStore.find(it)
                }
            )
        }
    }
}
