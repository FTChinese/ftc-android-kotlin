package com.ft.ftchinese.model.price

/**
 * CartItem is the item user put in shopping cart.
 * It contains the selected price user want to pay,
 * and optional discount available for current moment.
 * NOTE the discount might be different from the one nested in price since the actually valid discount might comes from other sources.
 */
data class CheckoutItem(
    val price: Price,
    val discount: Discount? = null,
) {
    val payableAmount: Double
        get() = price.unitAmount - (discount?.priceOff ?: 0.0)
}
