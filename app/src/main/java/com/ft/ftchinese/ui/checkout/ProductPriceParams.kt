package com.ft.ftchinese.ui.checkout

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import com.ft.ftchinese.model.subscription.Discount
import com.ft.ftchinese.model.subscription.Price
import com.ft.ftchinese.ui.formatter.PriceStringBuilder

data class ProductPriceParams(
    val price: Price,
    val discount: Discount? = null
)

/**
 * Converts the SelectedItem to human-readable content.
 */
data class ProductPriceUI(
    val productName: String,
    val payable: Spannable = SpannableString(""),         // The actually charged amount
    val original: Spannable? = null, // The original price if discount exists.
) {
    companion object {
        @JvmStatic
        fun from(ctx: Context, params: ProductPriceParams): ProductPriceUI {
            return ProductPriceUI(
                productName = ctx.getString(params.price.tier.stringRes),
                payable = PriceStringBuilder
                    .fromPrice(
                        price = params.price,
                        discount = params.discount
                    )
                    .withScale()
                    .build(ctx),
                original = if (params.discount != null) {
                    PriceStringBuilder
                        .fromPrice(
                            price = params.price
                        )
                        .withOriginal()
                        .withStrikeThrough()
                        .build(ctx)
                } else null
            )
        }
    }
}
