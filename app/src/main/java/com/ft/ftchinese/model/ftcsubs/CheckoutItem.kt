package com.ft.ftchinese.model.ftcsubs

import android.os.Parcelable
import com.ft.ftchinese.model.price.Discount
import com.ft.ftchinese.model.price.Price
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.request.OrderParams
import kotlinx.parcelize.Parcelize

/**
 * CartItem is the item user put in shopping cart.
 * It contains the selected price user want to pay,
 * and optional discount available for current moment.
 * NOTE the discount might be different from the one nested in price since the actually valid discount might comes from other sources.
 */
@Parcelize
data class CheckoutItem(
    val price: Price,
    val discount: Discount? = null,
): Parcelable {
    /**
     * Calculates the amount that should actually paid.
     */
    val payableAmount: Double
        get() = price.unitAmount - (discount?.priceOff ?: 0.0)

    val reqParams: OrderParams
        get() = OrderParams(
            priceId = price.id,
            discountId = discount?.id,
        )

    fun verify(isTest: Boolean): Boolean {
        return price.liveMode != isTest
    }
    
    companion object {
        @JvmStatic
        fun newInstance(price: Price, m: Membership): CheckoutItem {
            return CheckoutItem(
                price = price,
                discount = price.applicableOffer(m.offerKinds)
            )
        }
    }
}
