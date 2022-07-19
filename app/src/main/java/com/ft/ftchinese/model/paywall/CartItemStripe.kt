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

    fun payablePrice(): PriceParts {
        if (trial != null) {
            PriceParts(
                symbol = getCurrencySymbol(trial.currency),
                amount = convertCent(trial.unitAmount),
                period = trial.periodCount,
                isRecurring = false,
                highlighted = true
            )
        }

        if (coupon != null) {
            return PriceParts(
                symbol = getCurrencySymbol(coupon.currency),
                amount = convertCent(recurring.unitAmount - coupon.amountOff),
                period = recurring.periodCount,
                isRecurring = false,
                highlighted = true,
            )
        }

        return PriceParts(
            symbol = getCurrencySymbol(recurring.currency),
            amount = convertCent(recurring.unitAmount),
            period = recurring.periodCount,
            isRecurring = true,
            highlighted = true,
        )
    }

    fun overriddenPrice(): PriceParts? {
        if (trial == null && coupon == null) {
            return null
        }

        return PriceParts(
            symbol = getCurrencySymbol(recurring.currency),
            amount = convertCent(recurring.unitAmount),
            period = recurring.periodCount,
            isRecurring = true
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
