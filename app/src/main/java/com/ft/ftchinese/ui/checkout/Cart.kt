package com.ft.ftchinese.ui.checkout

import android.content.Context
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.subscription.Discount
import com.ft.ftchinese.model.subscription.Price

class Cart(
    val price: Price,
    val member: Membership,
) {
    private var discounts = mutableListOf<Discount>()
    private var discountIndex: Int = -1
    private var intents: CheckoutIntents

    init {
        if (price.promotionOffer.isValid()) {
            discounts.add(price.promotionOffer)
        }

        discountIndex = findMaxDiscount()

        intents = CheckoutIntents.newInstance(member, price.edition)
    }

    val productPriceParams: ProductPriceParams
        get() = ProductPriceParams(
            price = price,
            discount = selectedDiscount
        )

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
                payMethod = method,
                orderKind = it.orderKind,
                price = price,
                discount = selectedDiscount,
            )
        }
    }
}
