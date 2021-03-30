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
class CheckoutCounter(
    val price: Price,
    val member: Membership,
) : AnkoLogger {
    // Use might enjoy multiple discount at the same moment:
    // promotion offered by FTC, or reward for timely renewal.
//    private val discounts = mutableListOf<Discount>()
    val intents: CheckoutIntents = CheckoutIntents.newInstance(member, price.edition)
    // Get the selected price and optional discount.
    val item = CheckoutItem.newInstance(price, member)

    fun payMethodAllowed(method: PayMethod) = intents.payMethods.contains(method)

    fun payButtonParams(method: PayMethod): PayButtonParams? {
        return intents.findIntent(method)?.let {
            PayButtonParams(
                selectedPayMethod = method,
                orderKind = it.orderKind,
                price = price,
                discount = item.discount
            )
        }
    }
}
