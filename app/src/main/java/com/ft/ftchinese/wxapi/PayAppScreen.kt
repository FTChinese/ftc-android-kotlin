package com.ft.ftchinese.wxapi

import androidx.annotation.StringRes
import com.ft.ftchinese.R

enum class PayAppScreen(
    @StringRes val titleId: Int,
) {
    PayResponse(titleId = R.string.pay_brand_wechat),
    Invoices(titleId = R.string.title_latest_invoice),
    BuyerInfo(titleId = R.string.title_buyer_info);

    companion object {
        @JvmStatic
        fun fromRoute(route: String?): PayAppScreen =
            when (route?.substringBefore("/")) {
                PayResponse.name -> PayResponse
                Invoices.name -> Invoices
                BuyerInfo.name -> BuyerInfo
                null -> PayResponse
                else -> throw IllegalArgumentException("Route $route is not recognized")
            }
    }
}
