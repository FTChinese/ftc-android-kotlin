package com.ft.ftchinese.ui.checkout

import android.content.Context
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.price.CheckoutItem
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.price.Discount
import com.ft.ftchinese.model.price.Price
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
    private var discounts = mutableListOf<Discount>()
    private var discountIndex: Int = -1
    private var intents: CheckoutIntents

    init {
        // Determine if promotion discount is available for now.
        if (price.promotionOffer.isValid()) {
            discounts.add(price.promotionOffer)
        }

        discountIndex = findMaxDiscount()

        // Possible checkout intents prior to selecting a payment method.
        intents = CheckoutIntents.newInstance(member, price.edition)
    }

    // Get the selected price and optional discount.
    val checkoutItem: CheckoutItem
        get() = CheckoutItem(
            price = price,
            discount = selectedDiscount
        )

    // Show a spinner UI so that user choose a valid discount.
    val discountSpinnerParams: DiscountSpinnerParams
        get() = DiscountSpinnerParams(
            items = discounts,
            selectedIndex = discountIndex,
        )

    val checkoutIntents: CheckoutIntents
        get() = intents

    private val selectedDiscount: Discount?
        get() = if (discountIndex >= 0) {
            discounts[discountIndex]
        } else {
            null
        }

    private val payableAmount: Double
        get() = price.unitAmount - (selectedDiscount?.priceOff ?: 0.0)

    // By default we pre-select the discount with highest rate.
    private fun findMaxDiscount(): Int {
        if (discounts.size == 0) {
            return -1
        }
        if (discounts.size == 1) {
            return 0
        }

        var maxIndex = 0
        for (i in 1 until discounts.size) {
            if (discounts[i].priceOff == null || discounts[maxIndex].priceOff == null) {
                continue
            }
            if (discounts[i].priceOff!! > discounts[maxIndex].priceOff!!) {
                maxIndex = i
            }
        }

        return maxIndex
    }

    // Use the discount specified by user in case user manually changed the dropdown spinner.
    fun useDiscount(pos: Int) {
        if (pos > 0 && pos < discounts.size) {
            discountIndex = pos
        }
    }

    fun formatToolbarTitle(ctx: Context): String {
        return intents.orderKinds.joinToString("/") {
            ctx.getString(it.stringRes)
        }
    }

    fun payMethodAllowed(method: PayMethod) = intents.payMethods.contains(method)

    fun payButtonParams(method: PayMethod): PayButtonParams? {
        return intents.findIntent(method)?.let {
            PayButtonParams(
                selectedPayMethod = method,
                orderKind = it.orderKind,
                price = price,
                discount = selectedDiscount,
            )
        }
    }
}
