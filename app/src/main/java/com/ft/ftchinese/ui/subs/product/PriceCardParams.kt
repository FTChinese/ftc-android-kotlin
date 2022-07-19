package com.ft.ftchinese.ui.subs.product

import android.content.Context
import com.ft.ftchinese.R
import com.ft.ftchinese.model.paywall.CartItemFtc
import com.ft.ftchinese.model.paywall.CartItemStripe
import com.ft.ftchinese.model.paywall.PriceParts
import com.ft.ftchinese.ui.formatter.FormatHelper
import com.ft.ftchinese.ui.formatter.formatAmountOff

data class OverriddenPrice(
    val description: String,
    val parts: PriceParts,
)

data class PriceCardParams(
    val heading: String,
    val title: String?,
    val payable: PriceParts,
    val overridden: OverriddenPrice? = null,
    val isAutoRenew: Boolean,
    val smallPrint: String?,
) {
    companion object {
        @JvmStatic
        fun ofFtc(ctx: Context, item: CartItemFtc): PriceCardParams {
            val heading = if (item.isIntro)
                ctx.getString(R.string.price_heading_aliwx_trial)
            else
                ctx.getString(
                    R.string.price_heading_wxali,
                    FormatHelper.cycleOfYMD(ctx, item.normalizePeriod())
                )

            val smallPrint = ctx.getString(R.string.limited_to_aliwx)

            // For regular price with discount
            if (item.discount != null) {
                return PriceCardParams(
                    heading = heading,
                    title = item.discount.description,
                    payable = item.payablePrice(),
                    overridden = item.overriddenPrice()?.let {
                        OverriddenPrice(
                            description = ctx.getString(R.string.price_original_prefix),
                            parts = it,
                        )
                    },
                    isAutoRenew = false,
                    smallPrint = smallPrint,
                )
            }

            return PriceCardParams(
                heading = heading,
                title = null,
                payable = item.payablePrice(),
                isAutoRenew = false,
                smallPrint = smallPrint
            )
        }


        @JvmStatic
        fun ofStripe(ctx: Context, item: CartItemStripe): PriceCardParams {
            val heading = ctx.getString(
                R.string.price_heading_stripe,
                FormatHelper.cycleOfYMD(ctx, item.recurring.periodCount)
            )

            val smallPrint = ctx.getString(R.string.limited_to_stripe)

            if (item.trial != null) {
                return PriceCardParams(
                    heading = heading,
                    title = ctx.getString(R.string.price_heading_stripe_trial),
                    payable = item.payablePrice(),
                    overridden = item.overriddenPrice()?.let {
                        OverriddenPrice(
                            description = ctx.getString(R.string.price_original_stripe_prefix),
                            parts = it,
                        )
                    },
                    isAutoRenew = true,
                    smallPrint = smallPrint,
                )
            }

            if (item.coupon != null) {
                return PriceCardParams(
                    heading = heading,
                    title = "领取优惠 ${formatAmountOff(ctx, item.coupon.moneyParts)}",
                    payable = item.payablePrice(),
                    overridden = item.overriddenPrice()?.let {
                        OverriddenPrice(
                            description = ctx.getString(R.string.price_original_prefix),
                            parts = it,
                        )
                    },
                    isAutoRenew = true,
                    smallPrint = smallPrint,
                )
            }

            return PriceCardParams(
                heading = heading,
                title = null,
                payable = item.payablePrice(),
                isAutoRenew = true,
                smallPrint = smallPrint,
            )
        }
    }
}
