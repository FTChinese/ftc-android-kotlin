package com.ft.ftchinese.ui.components

import com.ft.ftchinese.R

enum class SubsScreen(
    val titleId: Int,
) {
    Paywall(
        titleId = R.string.title_subscription
    ),
    FtcPay(
        titleId = R.string.check_out
    ),
    StripePay(
        titleId = R.string.pay_method_stripe
    );

    companion object {
        fun fromRoute(route: String?): SubsScreen =
            when (route?.substringBefore("/")) {
                Paywall.name -> Paywall
                FtcPay.name -> FtcPay
                StripePay.name -> StripePay
                null -> Paywall
                else -> throw IllegalArgumentException("Route $route is not recognized")
            }

    }
}
