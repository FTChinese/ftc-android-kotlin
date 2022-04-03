package com.ft.ftchinese.model.paywall

import android.os.Parcelable
import com.ft.ftchinese.model.stripesubs.StripePrice
import kotlinx.parcelize.Parcelize

@Parcelize
data class CartItemStripe(
    val intent: CheckoutIntent,
    val recurring: StripePrice,
    val trial: StripePrice?,
) : Parcelable
