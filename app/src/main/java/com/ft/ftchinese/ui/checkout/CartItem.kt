package com.ft.ftchinese.ui.checkout

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import com.ft.ftchinese.R
import com.ft.ftchinese.model.paywall.CartItemFtc
import com.ft.ftchinese.model.paywall.CartItemStripe
import com.ft.ftchinese.ui.formatter.CartPriceFormatter
import com.ft.ftchinese.ui.formatter.FormatHelper

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
        fun ofStripe(ctx: Context, checkoutItem: CartItemStripe): CartItem {
            if (checkoutItem.trialPrice == null) {
                return CartItem(
                    productName = FormatHelper.getTier(ctx, checkoutItem.recurringPrice.tier),
                    payable = CartPriceFormatter(
                        currency = checkoutItem.recurringPrice.currency,
                        amount = checkoutItem.recurringPrice.moneyAmount,
                        period = checkoutItem.recurringPrice.periodCount,
                        isIntroductory = false,
                    )
                        .scaleAmount()
                        .format(ctx),
                    smallPrint = null,
                )
            }

            return CartItem(
                productName = FormatHelper.getTier(ctx, checkoutItem.recurringPrice.tier),
                payable = CartPriceFormatter(
                    currency = checkoutItem.trialPrice.currency,
                    amount = checkoutItem.trialPrice.moneyAmount,
                    period = checkoutItem.trialPrice.periodCount,
                    isIntroductory = true,
                )
                    .scaleAmount()
                    .format(ctx),
                smallPrint = SpannableString(
                    FormatHelper.stripeAutoRenewalMessage(
                        ctx = ctx,
                        price = checkoutItem.recurringPrice,
                        isTrial = true
                    )
                )
            )
        }

        @JvmStatic
        fun ofFtc(ctx: Context, item: CartItemFtc): CartItem {
            if (item.discount == null) {
                return CartItem(
                    productName = FormatHelper.getTier(ctx, item.price.tier),
                    payable = CartPriceFormatter(
                        currency = item.price.currency,
                        amount = item.price.unitAmount,
                        isIntroductory = item.isIntro,
                        period = item.price.normalizeYMD(),
                    )
                        .scaleAmount()
                        .format(ctx),
                    smallPrint = if (item.isIntro) {
                        SpannableString(item.price.title)
                    } else {
                        null
                    }
                )
            }

            // If discount exists, it must not be introductory.
            return CartItem(
                productName = FormatHelper.getTier(ctx, item.price.tier),
                payable = CartPriceFormatter(
                    currency = item.price.currency,
                    amount = item.payableAmount(),
                    isIntroductory = item.isIntro,
                    period = item.price.normalizeYMD(),
                )
                    .scaleAmount()
                    .format(ctx),
                smallPrint = CartPriceFormatter(
                    currency = item.price.currency,
                    amount = item.price.unitAmount,
                    isIntroductory = false,
                    period = item.price.normalizeYMD()
                )
                    .withPrefix(ctx.getString(R.string.prefix_original_price))
                    .withStrikeThrough()
                    .format(ctx)
            )
        }

    }
}
