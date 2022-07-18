package com.ft.ftchinese.model.paywall

import com.ft.ftchinese.model.ftcsubs.Discount
import com.ft.ftchinese.model.ftcsubs.Price
import com.ft.ftchinese.model.ftcsubs.YearMonthDay
import com.ft.ftchinese.model.reader.Membership

data class CartItemFtc(
    val intent: CheckoutIntent,
    val price: Price,
    val discount:  Discount? = null,
    val isIntro: Boolean, // Indicating trial price.
) {
    fun normalizePeriod(): YearMonthDay {
        if (discount != null && !discount.overridePeriod.isZero()) {
            return discount.overridePeriod
        }

        return price.periodCount
    }

    fun payableAmount(): Double {
        return price.unitAmount - (discount?.priceOff ?: 0.0)
    }

    fun payablePrice(): PriceParts {
        return PriceParts(
            symbol = price.currency,
            amount = if (discount != null) {
                price.unitAmount - (discount.priceOff ?: 0.0)
            } else {
                  price.unitAmount
            },
            period = normalizePeriod(),
            isRecurring = false,
            highlighted = true,
        )
    }

    fun overriddenPrice(): PriceParts? {
        if (discount == null) {
            return null
        }

        return PriceParts(
            symbol = price.currency,
            amount = price.unitAmount,
            period = price.periodCount,
            isRecurring = false,
            highlighted = false,
            crossed = true,
        )
    }

    companion object {

        @JvmStatic
        fun ofIntro(introPrice: Price?): CartItemFtc? {
            return if (introPrice == null || !introPrice.isValid()) {
                null
            } else {
                CartItemFtc(
                    intent = CheckoutIntent.newMember,
                    price = introPrice,
                    discount = null,
                    isIntro = true,
                )
            }
        }

        @JvmStatic
        fun newInstance(price: Price, m: Membership): CartItemFtc {
            val offerFilter = m.offerKinds

            return CartItemFtc(
                intent = CheckoutIntent.ofFtc(m, price),
                price = price,
                discount = price.filterOffer(offerFilter),
                isIntro = price.isIntro && price.isValid()
            )
        }
    }
}
