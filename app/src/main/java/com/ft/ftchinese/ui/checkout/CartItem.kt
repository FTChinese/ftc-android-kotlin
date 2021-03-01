package com.ft.ftchinese.ui.checkout

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import com.ft.ftchinese.model.price.CheckoutItem
import com.ft.ftchinese.ui.formatter.PriceStringBuilder

/**
 * Converts the CheckoutItem to human-readable content.
 */
data class CartItem(
    val productName: String, // The name of the product user selected.
    val payable: Spannable = SpannableString(""),  // The actually charged amount with highlight.
    val original: Spannable? = null, // The original price if discount exists. It is always crossed over.
) {
    companion object {
        @JvmStatic
        fun from(ctx: Context, item: CheckoutItem): CartItem {
            return CartItem(
                productName = ctx.getString(item.price.tier.stringRes),
                payable = PriceStringBuilder
                    .fromPrice(
                        price = item.price,
                        discount = item.discount
                    )
                    .withScale()
                    .build(ctx),
                original = if (item.discount != null) {
                    PriceStringBuilder
                        .fromPrice(
                            price = item.price
                        )
                        .withOriginal()
                        .withStrikeThrough()
                        .build(ctx)
                } else null
            )
        }
    }
}
