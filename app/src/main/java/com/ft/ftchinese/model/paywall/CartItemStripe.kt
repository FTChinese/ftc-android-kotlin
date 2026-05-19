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

    val isDowngrade: Boolean
        get() = intent.kind == IntentKind.Downgrade

    val isDirectSubscriptionUpdate: Boolean
        get() = intent.kind == IntentKind.Downgrade ||
            intent.kind == IntentKind.CancelScheduledChange

    val isForbidden: Boolean
        get() = intent.kind == IntentKind.Forbidden

    val requiresPaymentMethod: Boolean
        get() = !isDirectSubscriptionUpdate

    fun recurPrice(): PriceParts {
        return PriceParts(
            symbol = getCurrencySymbol(recurring.currency),
            amount = convertCent(recurring.unitAmount),
            period = recurring.periodCount,
            isRecurring = true,
            highlighted = true,
        )
    }

    fun payableAmountInMinorUnits(): Int {
        if (trial != null) {
            return trial.unitAmount
        }

        if (coupon != null) {
            return (recurring.unitAmount - coupon.amountOff).coerceAtLeast(0)
        }

        return recurring.unitAmount
    }

    fun payableAmount(): Double {
        return convertCent(payableAmountInMinorUnits())
    }

    fun subsParams(
        payMethod: StripePaymentMethod?,
        prorationDate: Long? = null,
    ): SubParams {
        return SubParams(
            priceId = recurring.id,
            introductoryPriceId = trial?.id,
            coupon = if (isDirectSubscriptionUpdate) null else coupon?.id,
            defaultPaymentMethod = if (isDirectSubscriptionUpdate) null else payMethod?.id,
            currency = recurring.currency.takeIf { it.isNotBlank() },
            prorationDate = prorationDate,
        )
    }
}
