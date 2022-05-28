package com.ft.ftchinese.ui.checkout

import androidx.annotation.StringRes
import com.ft.ftchinese.R

enum class CheckoutScreen(
    @StringRes val titleId: Int,
) {
    Invoices(titleId = R.string.title_latest_invoice),
    BuyerInfo(titleId = R.string.title_buyer_info);

    companion object {
        fun fromRoute(route: String?): CheckoutScreen =
            when (route?.substringBefore("/")) {
                Invoices.name -> Invoices
                BuyerInfo.name -> BuyerInfo
                null -> Invoices
                else -> throw IllegalArgumentException("Route $route is not recognized")
            }
    }
}
