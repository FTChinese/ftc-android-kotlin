package com.ft.ftchinese.model.paywall

import com.ft.ftchinese.model.stripesubs.StripeCoupon
import com.ft.ftchinese.model.stripesubs.StripePaymentMethod
import com.ft.ftchinese.model.stripesubs.StripePrice
import com.ft.ftchinese.model.stripesubs.SubParams

data class CartItemStripe(
    val intent: CheckoutIntent,
    val recurring: StripePrice,
    val trial: StripePrice?,
    val coupon: StripeCoupon?,
) {

    val isApplyCoupon: Boolean
        get() = intent.kind == IntentKind.ApplyCoupon

    val isForbidden: Boolean
        get() = intent.kind == IntentKind.Forbidden

    fun recurPrice(): PriceParts {
        return PriceParts(
            symbol = getCurrencySymbol(recurring.currency),
            amount = convertCent(recurring.unitAmount),
            period = recurring.periodCount,
            isRecurring = true,
            highlighted = true,
        )
    }

    fun subsParams(
        payMethod: StripePaymentMethod?,
        idemKey: String?
    ): SubParams {
        return SubParams(
            priceId = recurring.id,
            introductoryPriceId = trial?.id,
            coupon = coupon?.id,
            defaultPaymentMethod = payMethod?.id,
            idempotency = idemKey,
        )
    }
}
