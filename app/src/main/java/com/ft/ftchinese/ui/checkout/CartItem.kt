package com.ft.ftchinese.ui.checkout

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.PriceSource
import com.ft.ftchinese.model.paywall.CheckoutPrice
import com.ft.ftchinese.ui.formatter.CartFormatter

/**
 * Converts the CheckoutItem to human-readable content.
 */
data class CartItem(
    val productName: String, // The name of the product user selected.
    val payable: Spannable = SpannableString(""),  // The actually charged amount with highlight.
    val smallPrint: Spannable? = null, // The original price if discount exists. It is always crossed over.
) {
    companion object {
        @JvmStatic
        fun newInstance(ctx: Context, price: CheckoutPrice): CartItem {
            return CartItem(
                productName = ctx.getString(price.regular.tier.stringRes),
                payable = CartFormatter
                    .newInstance(price.favour ?: price.regular)
                    .withScale()
                    .format(ctx),
                smallPrint = price.favour?.let {
                    if (it.source == PriceSource.Ftc) {
                        CartFormatter
                            .newInstance(price.regular)
                            .withPrefix(ctx.getString(R.string.prefix_original_price))
                            .withStrikeThrough()
                            .format(ctx)
                    } else {
                        CartFormatter
                            .newInstance(price.regular)
                            .withPrefix(ctx.getString(R.string.auto_renew_after_trial))
                            .format(ctx)
                    }
                }
            )
        }
    }
}
