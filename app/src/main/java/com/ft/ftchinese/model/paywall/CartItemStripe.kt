package com.ft.ftchinese.model.paywall

import com.ft.ftchinese.model.enums.OrderKind
import com.ft.ftchinese.model.stripesubs.StripePrice

data class CartItemStripe(
    val orderKind: OrderKind,
    val recurringPrice: StripePrice,
    val trialPrice: StripePrice?,
)

data class CartItemStripeV2(
    val intent: CheckoutIntent,
    val recurring: StripePrice,
    val trial: StripePrice?,
)
