package com.ft.ftchinese.ui.subs

import androidx.annotation.StringRes
import com.ft.ftchinese.R

enum class SubsAppScreen(
    @StringRes val titleId: Int,
) {
    Paywall(titleId = R.string.title_subscription),
    FtcPay(titleId = R.string.check_out),
    StripePay(titleId = R.string.pay_method_stripe),
    Invoices(titleId = R.string.title_latest_invoice),
    BuyerInfo(titleId = R.string.title_buyer_info);

    companion object {
        @JvmStatic
        fun fromRoute(route: String?): SubsAppScreen =
            when (route?.substringBefore("/")) {
                Paywall.name -> Paywall
                FtcPay.name -> FtcPay
                StripePay.name -> StripePay
                Invoices.name -> Invoices
                BuyerInfo.name -> BuyerInfo
                null -> Paywall
                else -> throw IllegalArgumentException("Route $route is not recognized")
            }

    }
}
