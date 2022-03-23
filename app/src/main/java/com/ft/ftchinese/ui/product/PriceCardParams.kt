package com.ft.ftchinese.ui.product

import android.content.Context
import com.ft.ftchinese.model.paywall.CartItemFtcV2
import com.ft.ftchinese.model.paywall.CartItemStripeV2
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
        fun ofFtc(ctx: Context, item: CartItemFtcV2): PriceCardParams {
            val heading = if (item.isIntro)
                "试用"
            else
                "包${FormatHelper.cycleOfYMD(ctx, item.normalizePeriod())}"

            val currencySymbol = PriceParts.findSymbol(item.price.currency)

            val smallPrint = "* 仅限支付宝或微信支付"

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
                        notes = "原价",
                    ),
                    isAutoRenew = false,
                    smallPrint = smallPrint,
                )
            }

            return PriceCardParams(
                heading = heading,
                title = item.price.title,
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

        fun ofStripe(ctx: Context, item: CartItemStripeV2): PriceCardParams {
            val heading = "连续包${FormatHelper.cycleOfYMD(ctx, item.recurring.periodCount)}"

            val currencySymbol = PriceParts.findSymbol(item.recurring.currency)

            val smallPrint = "* 仅限Stripe支付"

            if (item.trial != null) {
                return PriceCardParams(
                    heading = heading,
                    title = "新会员首次试用",
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
                        notes = "试用结束后自动续订"
                    ),
                    isAutoRenew = true,
                    smallPrint = smallPrint,
                )
            }

            return PriceCardParams(
                heading = heading,
                title = "",
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
