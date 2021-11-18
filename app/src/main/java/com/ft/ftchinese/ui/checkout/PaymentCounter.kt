package com.ft.ftchinese.ui.checkout

import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.paywall.CheckoutPrice
import com.ft.ftchinese.model.reader.Membership

/**
 * PaymentCounter contains the price user want to checkout,
 * and the purchase intents allowed for this session.
 */
data class PaymentCounter (
    val price: CheckoutPrice,
    val intents: CheckoutIntents, // Deduced intents.
) {
    fun selectPaymentMethod(method: PayMethod): PaymentIntent? {
        return intents.findIntent(method)?.let {
            PaymentIntent(
                price = price,
                orderKind = it.orderKind,
                payMethod = method
            )
        }
    }

    companion object {
        @JvmStatic
        fun newFtcInstance(price: CheckoutPrice, m: Membership): PaymentCounter {
            return PaymentCounter(
                price = price,
                intents = CheckoutIntents.newInstance(m, price.regular),
            )
        }

        @JvmStatic
        fun newStripeInstance(price: CheckoutPrice, m: Membership): PaymentCounter {
            return PaymentCounter(
                price = price,
                intents = CheckoutIntents.newInstance(m, price.regular),
            )
        }
    }
}


