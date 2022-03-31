package com.ft.ftchinese.model.paywall

import android.os.Parcelable
import com.ft.ftchinese.model.enums.OrderKind
import com.ft.ftchinese.model.stripesubs.StripePrice
import kotlinx.parcelize.Parcelize

@Deprecated("")
data class CartItemStripe(
    val orderKind: OrderKind,
    val recurringPrice: StripePrice,
    val trialPrice: StripePrice?,
)

// TODO: removed parcelable.
@Parcelize
data class CartItemStripeV2(
    val intent: CheckoutIntent,
    val recurring: StripePrice,
    val trial: StripePrice?,
) : Parcelable
