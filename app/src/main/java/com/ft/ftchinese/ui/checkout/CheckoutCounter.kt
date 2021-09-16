package com.ft.ftchinese.ui.checkout

import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.ftcsubs.CheckoutItem
import com.ft.ftchinese.model.price.Price
import com.ft.ftchinese.model.reader.Membership
import org.jetbrains.anko.AnkoLogger

/**
 * CheckoutCounter is used to build the CheckOutActivity based on the
 * price user chosen and current subscription status.
 */
@Deprecated("")
class CheckoutCounter(
    val price: Price,
    val member: Membership,
) : AnkoLogger {
    // Use might enjoy multiple discount at the same moment:
    // promotion offered by FTC, or reward for timely renewal.
//    private val discounts = mutableListOf<Discount>()
    @Deprecated("")
    val intents: CheckoutIntents = CheckoutIntents.newInstance(member, price.edition)
    // Get the selected price and optional discount.
    @Deprecated("")
    val item = CheckoutItem.newInstance(price, member)
}

data class PaymentCounter (
    val item: CheckoutItem,
    val intents: CheckoutIntents, // Deduced intents.
) {
    fun selectPaymentMethod(method: PayMethod): PaymentIntent? {
        return intents.findIntent(method)?.let {
            PaymentIntent(
                item = item,
                orderKind = it.orderKind,
                payMethod = method
            )
        }
    }

    companion object {
        @JvmStatic
        fun newFtcInstance(item: CheckoutItem, m: Membership): PaymentCounter {
            return PaymentCounter(
                item = item,
                intents = CheckoutIntents.newInstance(m, item.price.edition)
            )
        }

        @JvmStatic
        fun newStripeInstance(price: Price, m: Membership): PaymentCounter {
            return PaymentCounter(
                item = CheckoutItem(
                    price = price,
                    discount = null,
                ),
                intents = CheckoutIntents.newInstance(m, price.edition),
            )
        }
    }
}


