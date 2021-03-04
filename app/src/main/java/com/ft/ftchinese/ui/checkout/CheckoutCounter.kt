package com.ft.ftchinese.ui.checkout

import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.price.CheckoutItem
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
    val checkoutIntents: CheckoutIntents = CheckoutIntents.newInstance(member, price.edition)

    val discountOptions: DiscountOptions = DiscountOptions(if (price.promotionOffer.isValid()) {
        listOf(price.promotionOffer)
    } else listOf())

    // Get the selected price and optional discount.
    val checkoutItem: CheckoutItem
        get() = CheckoutItem(
            price = price,
            discount = discountOptions.discountSelected
        )

    // Use the discount specified by user in case user manually changed the dropdown spinner.
    fun useDiscount(pos: Int) {
        discountOptions.changeDiscount(pos)
    }

    fun payMethodAllowed(method: PayMethod) = checkoutIntents.payMethods.contains(method)

    fun payButtonParams(method: PayMethod): PayButtonParams? {
        return checkoutIntents.findIntent(method)?.let {
            PayButtonParams(
                selectedPayMethod = method,
                orderKind = it.orderKind,
                price = price,
                discount = discountOptions.discountSelected
            )
        }
    }
}
