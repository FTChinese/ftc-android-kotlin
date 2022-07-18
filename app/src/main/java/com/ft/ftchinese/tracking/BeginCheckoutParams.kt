package com.ft.ftchinese.tracking

import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.paywall.CartItemFtc
import com.ft.ftchinese.model.paywall.CartItemStripe
import com.ft.ftchinese.model.paywall.convertCent

data class BeginCheckoutParams(
    val value: Double,
    val currency: String,
    val payMethod: PayMethod,
) {
    companion object {
        @JvmStatic
        fun ofFtc(item: CartItemFtc, method: PayMethod): BeginCheckoutParams {
            return BeginCheckoutParams(
                value = item.payableAmount(),
                currency = item.price.currency,
                payMethod = method
            )
        }

        @JvmStatic
        fun ofStripe(item: CartItemStripe): BeginCheckoutParams {
            return BeginCheckoutParams(
                value = if (item.trial != null) {
                    convertCent(item.trial.unitAmount)
                } else {
                    convertCent((item.recurring.unitAmount))
                },
                currency = item.recurring.currency,
                payMethod = PayMethod.STRIPE,
            )
        }
    }
}
