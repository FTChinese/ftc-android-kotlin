package com.ft.ftchinese.ui.checkout

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import com.ft.ftchinese.R
import com.ft.ftchinese.model.paywall.FtcCheckout
import com.ft.ftchinese.model.paywall.StripeCounter
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
        fun ofStripe(ctx: Context, counter: StripeCounter): CartItem {
            if (counter.trialPrice == null) {
                return CartItem(
                    productName = FormatHelper.getTier(ctx, counter.recurringPrice.tier),
                    payable = CartPriceFormatter(
                        currency = counter.recurringPrice.currency,
                        amount = counter.recurringPrice.moneyAmount,
                        period = counter.recurringPrice.periodCount,
                        isIntroductory = false,
                    )
                        .scaleAmount()
                        .format(ctx),
                    smallPrint = null,
                )
            }

            return CartItem(
                productName = FormatHelper.getTier(ctx, counter.recurringPrice.tier),
                payable = CartPriceFormatter(
                    currency = counter.trialPrice.currency,
                    amount = counter.trialPrice.moneyAmount,
                    period = counter.trialPrice.periodCount,
                    isIntroductory = true,
                )
                    .scaleAmount()
                    .format(ctx),
                smallPrint = SpannableString(
                    FormatHelper.stripeAutoRenewalMessage(
                        ctx = ctx,
                        price = counter.recurringPrice,
                        isTrial = true
                    )
                )
            )
        }

        @JvmStatic
        fun ofFtc(ctx: Context, item: FtcCheckout): CartItem {
            if (item.discount == null) {
                return CartItem(
                    productName = FormatHelper.getTier(ctx, item.price.tier),
                    payable = CartPriceFormatter(
                        currency = item.price.currency,
                        amount = item.price.unitAmount,
                        isIntroductory = item.isIntroductory,
                        period = item.price.normalizeYMD(),
                    )
                        .scaleAmount()
                        .format(ctx),
                    smallPrint = if (item.isIntroductory) {
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
                    isIntroductory = item.isIntroductory,
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
