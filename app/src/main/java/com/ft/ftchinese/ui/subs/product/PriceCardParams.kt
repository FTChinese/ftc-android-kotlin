package com.ft.ftchinese.ui.subs.product

import android.content.Context
import com.ft.ftchinese.R
import com.ft.ftchinese.model.paywall.CartItemFtc
import com.ft.ftchinese.model.paywall.CartItemStripe
import com.ft.ftchinese.model.paywall.PriceParts
import com.ft.ftchinese.ui.formatter.FormatHelper
import com.ft.ftchinese.ui.formatter.PeriodFormatter

data class PriceCardParams(
    val heading: String,
    val title: String?,
    val payable: PriceParts,
    val original: PriceParts?,
    val isAutoRenew: Boolean,
    val smallPrint: String?,
) {
    companion object {
        fun ofFtc(ctx: Context, item: CartItemFtc): PriceCardParams {
            val heading = if (item.isIntro)
                ctx.getString(R.string.price_heading_aliwx_trial)
            else
                ctx.getString(
                    R.string.price_heading_wxali,
                    FormatHelper.cycleOfYMD(ctx, item.normalizePeriod())
                )

            val currencySymbol = PriceParts.findSymbol(item.price.currency)

            val smallPrint = ctx.getString(R.string.limited_to_aliwx)

            // For regular price with discount
            if (item.discount != null) {
                return PriceCardParams(
                    heading = heading,
                    title = item.discount.description,
                    payable = PriceParts(
                        symbol = currencySymbol,
                        amount = FormatHelper.formatMoney(ctx, item.payableAmount()),
                        cycle = PeriodFormatter(
                            ymd = item.normalizePeriod()
                        ).format(ctx, false),
                    ),
                    original = PriceParts(
                        symbol = currencySymbol,
                        amount = FormatHelper.formatMoney(ctx, item.price.unitAmount),
                        cycle = PeriodFormatter(
                            ymd = item.price.periodCount
                        ).format(ctx, false),
                        notes = ctx.getString(R.string.price_original_aliwx_prefix),
                    ),
                    isAutoRenew = false,
                    smallPrint = smallPrint,
                )
            }

            return PriceCardParams(
                heading = heading,
                title = null,
                payable = PriceParts(
                    symbol = currencySymbol,
                    amount = FormatHelper.formatMoney(ctx, item.price.unitAmount),
                    cycle = PeriodFormatter(
                        ymd = item.price.periodCount
                    ).format(ctx, false),
                ),
                original = null,
                isAutoRenew = false,
                smallPrint = smallPrint
            )
        }

        fun ofStripe(ctx: Context, item: CartItemStripe): PriceCardParams {
            val heading = ctx.getString(
                R.string.price_heading_stripe,
                FormatHelper.cycleOfYMD(ctx, item.recurring.periodCount)
            )

            val currencySymbol = PriceParts.findSymbol(item.recurring.currency)

            val smallPrint = ctx.getString(R.string.limited_to_stripe)

            if (item.trial != null) {
                return PriceCardParams(
                    heading = heading,
                    title = ctx.getString(R.string.price_heading_stripe_trial),
                    payable = PriceParts(
                        symbol = currencySymbol,
                        amount = FormatHelper.formatMoney(ctx, item.trial.moneyAmount),
                        cycle = PeriodFormatter(
                            ymd = item.trial.periodCount
                        ).format(ctx, false)
                    ),
                    original = PriceParts(
                        symbol = currencySymbol,
                        amount = FormatHelper.formatMoney(ctx, item.recurring.moneyAmount),
                        cycle = PeriodFormatter(
                            ymd = item.recurring.periodCount
                        ).format(ctx, true),
                        notes = ctx.getString(R.string.price_original_stripe_prefix)
                    ),
                    isAutoRenew = true,
                    smallPrint = smallPrint,
                )
            }

            return PriceCardParams(
                heading = heading,
                title = null,
                payable = PriceParts(
                    symbol = currencySymbol,
                    amount = FormatHelper.formatMoney(ctx, item.recurring.moneyAmount),
                    cycle = PeriodFormatter(
                        ymd = item.recurring.periodCount
                    ).format(ctx, true)
                ),
                original = null,
                isAutoRenew = true,
                smallPrint = smallPrint,
            )
        }
    }
}
